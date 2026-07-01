<div align="center">

<img src="https://github.com/user-attachments/assets/3eb5e54f-0dee-4368-86fe-16bd94615f07" width="120" height="120" style="border-radius: 24px;" alt="HyperIsland Icon"/>

# HyperCopy

**复制后直达目标 App 的 Android 链接跳转增强模块**

[![GitHub Release](https://img.shields.io/github/v/release/1812z/HyperCopy?style=flat-square&logo=github&color=black)](https://github.com/1812z/HyperCopy/releases)
![Downloads](https://img.shields.io/github/downloads/1812z/HyperCopy/total?style=flat-square)
[![Platform](https://img.shields.io/badge/Platform-Android-green?style=flat-square&logo=android)](https://android.com)
[![LSPosed](https://img.shields.io/badge/Framework-LSPosed-blueviolet?style=flat-square)](https://github.com/LSPosed/LSPosed)
[![Shizuku](https://img.shields.io/badge/Support-Shizuku-2196F3?style=flat-square)](https://shizuku.rikka.app/)
[![Build](https://img.shields.io/badge/Build-Kotlin%20%2B%20Compose-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org)

</div>

---

## 功能介绍
<table>
<tr>
<td width="50%">

### 复制直达
监听复制内容，命中规则后快速跳转到目标 App，节省手动打开、搜索、粘贴的时间。

</td>
<td width="50%">

### 云规则
内置云规则页，可在线获取规则，支持搜索、下载与一键配置。

</td>
</tr>
<tr>
<td width="50%">

### 实时通知
复制命中规则后可通过安卓实时通知确认跳转，也可选择小米超级岛样式提示。

</td>
<td width="50%">

### LSPosed / Shizuku 支持
支持免 Root 的 Shizuku 方案，也支持 Root / LSPosed 监听剪贴板变化。

</td>
</tr>
</table>



---

## 使用说明

1. 安装 HyperCopy。
2. 按使用环境选择监听方案：`LSPosed` 或 `Shizuku`。
3. 在“云规则”页下载常用应用规则，或在“规则”页手动添加规则。
4. 复制链接、地址、快递单号或指定文本。
5. 命中规则后，HyperCopy 会按设置直接跳转或弹出确认通知。

---

## 规则能力

规则保存在应用私有目录的 `rules.json` 中，核心字段包括：

```json
{
  "name": "bilibili",
  "category": "link",
  "enabled": true,
  "actionMode": "direct_open",
  "matchRegex": ".*bilibili\\.com.*|.*b23\\.tv.*",
  "target": {
    "type": "url",
    "template": "",
    "packageName": "tv.danmaku.bili"
  }
}
```

支持的模板变量：

- `${p1}`、`${p2}`：`parameterRegex` 按顺序提取的捕获组。
- `${r1}`、`${r1_1}`：`extractionRegexes` 提取到的捕获组。
- `${input}`：完整复制内容。
- `${url:input}`：从完整复制内容中提取第一个 URL。
- `${redirectUrl}`：跳转后解析得到的重定向 URL。
- `${raw:变量名}`：原样插入参数，不做 URL 编码。

---

## 构建

确保已安装 Android Studio、JDK 17 和 Android SDK，然后运行：

```bash
gradle assembleRelease
```
---

## Star History

<a href="https://www.star-history.com/?repos=1812z%2FHyperCopy&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/image?repos=1812z/HyperCopy&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/image?repos=1812z/HyperCopy&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/image?repos=1812z/HyperCopy&type=date&legend=top-left" />
 </picture>
</a>

---

## 许可证

许可证文件待补充。欢迎 Issue 与 PR。

<div align="center">

Made with ❤️ for Android users

[![Star History](https://img.shields.io/github/stars/1812z/HyperCopy?style=social)](https://github.com/1812z/HyperCopy)

</div>
