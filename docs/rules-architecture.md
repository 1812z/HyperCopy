# 规则架构

HyperCopy 的规则配置保存在应用私有目录的 `rules.json`。UI、复制监听和 Xposed Hook 都只依赖同一套规则模型，避免浏览器学习逻辑和实际跳转逻辑耦合。

## JSON 格式

```json
{
  "version": 1,
  "rules": [
    {
      "id": "uuid",
      "name": "bilibili 跳转",
      "enabled": true,
      "actionMode": "parse_and_open",
      "matchRegex": ".*BV1xx.*",
      "parameterRegex": ".*(BV1xx).*",
      "target": {
        "type": "url",
        "template": "bilibili://video/${p1}",
        "packageName": "tv.danmaku.bili",
        "action": "android.intent.action.VIEW"
      },
      "sourceUrl": "https://www.bilibili.com/video/BV1xx",
      "createdAt": 1710000000000
    }
  ]
}
```

## 运行流程

1. 复制监听拿到剪贴板文本。
2. `RuleRepository.readRules()` 读取 `rules.json`。
3. `findRule(text, rules)` 先找命中的启用规则。
4. 根据 `actionMode` 选择执行方式。
5. `parse_and_open`：`parameterRegex` 提取捕获组，捕获组按顺序命名为 `${p1}`、`${p2}`，并替换 `target.template`。
6. `direct_open`：不解析参数，直接用复制内容打开，模板可使用 `${input}`。
7. `webview_resolve_and_open`：不显示浏览器界面，用隐藏 WebView 加载复制 URL，拦截 `bilibili://`、`snssdk1128://`、`intent://` 等非网页跳转后打开。

## 执行类型

`parse_and_open` 适合复制内容本身就包含 ID 的场景，例如 `BV...` 或明确的视频 ID。

`direct_open` 适合目标 App 自己能识别链接的场景，例如短链接无法解析参数时直接 `ACTION_VIEW` 打开原 URL。

`webview_resolve_and_open` 适合短链接必须经过网页 JS、meta refresh 或 App 打开按钮才能得到 deep link 的场景。运行时使用隐藏 WebView，不弹出浏览器界面。

## 内置浏览器学习流程

1. 规则页右下角 `+` 弹出菜单。
2. 选择“模拟浏览器”打开 `RuleBrowserActivity`。
3. 选择“添加规则”打开手动规则编辑页。
4. 模拟浏览器中输入分享链接或网页地址。
5. WebView 加载网页。
6. 用户点击网页中的“在 App 中打开”。
7. WebView 拦截非 `http/https` 跳转，例如 `bilibili://...` 或 `intent://...`。
8. 页面展示源网页 URL 和目标跳转，内容可长按复制。
9. 点击“添加规则”跳到手动规则编辑页并预填来源和跳转。

## 后续扩展

- 增加规则编辑页，允许手动修改 `matchRegex`、`parameterRegex`、`target.template` 和 `packageName`。
- 增加剪贴板监听服务或 Xposed Hook 调用 `matchRule()`。
- 增加多参数模板，例如 `${p1}` 作为视频 ID，`${p2}` 作为时间戳。
- 增加导入/导出 JSON。

## 剪贴板监听方案

HyperCopy 同时支持免 root 和 root/LSPosed 时，应该把剪贴板来源和规则执行解耦。监听层只产出“普通文本剪贴板事件”，规则匹配、去重、跳转都放在 App 进程内复用 `RuleRepository` 和 `RuleEngine`。

### 免 root 路径

使用前台服务注册 `ClipboardManager.OnPrimaryClipChangedListener`。

流程：

1. 用户开启“剪贴板监听”。
2. App 启动前台服务，显示常驻通知，降低被后台清理概率。
3. 服务注册 `OnPrimaryClipChangedListener`。
4. 回调中只读取 `primaryClip` 的第一条 `ClipData.Item.text` 或 `coerceToText()`。
5. 过滤空文本、超长文本、重复文本、非纯文本剪贴板。
6. 调用统一的剪贴板事件处理入口匹配规则并打开目标 App。

限制：Android 10 以后后台读取剪贴板受限制，免 root 方案必须尽量保持前台服务和可见通知；不同厂商仍可能杀后台，需要引导用户关闭省电限制、允许后台活动、自启动。

### root/LSPosed 路径

Hook 系统剪贴板服务，不在 Hook 进程里执行复杂规则和跳转，只把普通文本事件转发给 HyperCopy App。

推荐 Hook 点：系统进程中的 `com.android.server.clipboard.ClipboardService`，优先拦截设置剪贴板后的稳定方法，例如 `setPrimaryClip` 或内部分发监听的方法。不同 Android 版本签名可能变化，需要按系统版本做反射匹配。

流程：

1. LSPosed 作用域选择 Android 系统。
2. Hook `ClipboardService` 的写入剪贴板路径。
3. 从参数或服务状态中取得 `ClipData`。
4. 只接受 `MIMETYPE_TEXT_PLAIN`、`MIMETYPE_TEXT_HTML` 或可 `coerceToText()` 的单条文本。
5. 忽略 URI、Intent、图片、文件、多媒体和小米剪贴板扩展内容。
6. 对文本做长度上限和去重。
7. 通过显式 `Intent`、广播、ContentProvider 或 libxposed service 通知 HyperCopy App 处理。

不要把正则匹配和跳转逻辑直接放在 Hook 端。原因是 Hook 运行在系统进程，复杂正则、WebView 解析、启动第三方 App、读取 App 私有规则文件都会增加系统进程不稳定风险，也会带来跨进程配置同步问题。

### 文本过滤规则

监听层只传递普通文本，建议统一使用以下过滤：

1. `clipData == null` 或 `itemCount == 0` 时忽略。
2. `description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)` 或 `MIMETYPE_TEXT_HTML` 才优先处理。
3. `item.uri != null`、`item.intent != null` 时忽略，避免处理图片、文件、分享 Intent。
4. 文本 `trim()` 后为空忽略。
5. 文本超过上限忽略，建议先用 `8192` 或 `16384` 字符，避免超长文本拖慢正则。
6. 与最近一次处理文本相同且时间间隔很短时忽略，避免系统重复派发。

### 统一处理入口

后续实现时建议增加一个 App 进程内入口，例如 `ClipboardTextHandler.handle(context, text, source)`。

职责：

1. 做最终过滤和去重。
2. 读取 `RuleRepository(context).readRules()`。
3. 对 `ParseAndOpen` 调用 `matchRule()`。
4. 对 `DirectOpen` 调用 `findRule()` 后生成 `directIntent()`。
5. 对 `WebViewResolveAndOpen` 交给隐藏 WebView 解析入口。
6. 启动目标 `Intent`，失败时给出通知或记录日志。

这样免 root 服务、LSPosed Hook、未来的分享入口都只需要把文本交给同一个入口，行为一致，也便于测试。
