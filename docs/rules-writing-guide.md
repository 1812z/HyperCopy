# 规则编写指南

## 规则结构

每条规则包含以下核心字段：

```json
{
  "id": "uuid",
  "name": "规则名称",
  "category": "link|address|express",
  "enabled": true,
  "actionMode": "parse_and_open|direct_open|webview_resolve_and_open",
  "matchRegex": "匹配正则",
  "parameterRegex": "参数提取正则",
  "triggerRegexes": ["触发正则数组"],
  "extractionRegexes": ["提取正则数组"],
  "parseAfterRedirect": false,
  "target": {
    "type": "url|intent",
    "template": "目标模板",
    "packageName": "目标包名",
    "action": "android.intent.action.VIEW"
  },
  "clearClipboardAfterJump": false,
  "createdAt": 1710000000000
}
```

## 执行模式

1. **parse_and_open**：从复制内容中提取参数，替换模板后打开
2. **direct_open**：直接用复制内容打开目标
3. **webview_resolve_and_open**：用隐藏WebView解析短链接后打开

## 正则编写规则

### matchRegex
- 用于判断文本是否匹配规则
- 示例：`.*BV1\w+.*` 匹配包含BV号的内容

### parameterRegex
- 用于提取参数，捕获组按顺序命名为 `${p1}`, `${p2}`
- 示例：`.*(BV1\w+).*` 提取BV号到 `${p1}`

### triggerRegexes（可选）
- 替代matchRegex的触发条件数组
- 任一匹配即触发

### extractionRegexes（可选）
- 替代parameterRegex的提取规则数组
- 支持多模式提取

## 模板变量

- `${p1}`, `${p2}` - 按顺序的捕获组参数
- `${r1}` - 第一个提取规则的第一个捕获组
- `${r1_1}` - 第一个提取规则的第一个捕获组（精确索引）
- `${input}` - 原始输入文本
- `${redirectUrl}` - WebView解析后的重定向URL
- `${raw:变量名}` - 不进行URI编码的原始值

## 示例规则

### Bilibili视频跳转
```json
{
  "name": "Bilibili视频",
  "category": "link",
  "actionMode": "parse_and_open",
  "matchRegex": ".*BV1\\w+.*",
  "parameterRegex": ".*(BV1\\w+).*",
  "target": {
    "type": "url",
    "template": "bilibili://video/${p1}",
    "packageName": "tv.danmaku.bili"
  }
}
```

### 地址跳转
```json
{
  "name": "高德地图地址",
  "category": "address",
  "actionMode": "webview_resolve_and_open",
  "matchRegex": ".*amap\\.com.*",
  "parameterRegex": "",
  "target": {
    "type": "url",
    "template": "${redirectUrl}",
    "packageName": "com.autonavi.minimap"
  }
}
```

### 直接打开App
```json
{
  "name": "打开微信",
  "category": "link",
  "actionMode": "direct_open",
  "matchRegex": "",
  "parameterRegex": "",
  "target": {
    "type": "url",
    "template": "",
    "packageName": "com.tencent.mm"
  }
}
```

## 注意事项

1. 正则使用Kotlin/Java正则语法
2. 特殊字符需转义（如 `\.` 匹配点号）
3. 测试正则时使用在线工具验证
4. 优先使用`triggerRegexes`和`extractionRegexes`提高灵活性
5. WebView模式适用于需要网页解析的短链接