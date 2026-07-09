<div align="center">

<img src="https://github.com/user-attachments/assets/3eb5e54f-0dee-4368-86fe-16bd94615f07" width="120" height="120" style="border-radius: 24px;" alt="HyperIsland Icon"/>

# HyperCopy

**An Android link-jump enhancement module that opens the target app right after copying**

[![GitHub Release](https://img.shields.io/github/v/release/1812z/HyperCopy?style=flat-square&logo=github&color=black)](https://github.com/1812z/HyperCopy/releases)
![Downloads](https://img.shields.io/github/downloads/1812z/HyperCopy/total?style=flat-square)
[![Platform](https://img.shields.io/badge/Platform-Android-green?style=flat-square&logo=android)](https://android.com)
[![LSPosed](https://img.shields.io/badge/Framework-LSPosed-blueviolet?style=flat-square)](https://github.com/LSPosed/LSPosed)
[![Shizuku](https://img.shields.io/badge/Support-Shizuku-2196F3?style=flat-square)](https://shizuku.rikka.app/)
[![Build](https://img.shields.io/badge/Build-Kotlin%20%2B%20Compose-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org)

**English** | **[简体中文](README.md)**

</div>

---

## Features

<table>
<tr>
<td width="50%">

### Copy To Open
Listens to copied content and quickly jumps to the target app when a rule matches, saving the time spent manually opening, searching, and pasting.

</td>
<td width="50%">

### Cloud Rules
Includes a cloud rules page for fetching rules online, with search, download, and one-tap configuration support.

</td>
</tr>

<tr>
<td width="50%">

### Real-Time Notifications
After copied content matches a rule, Android real-time notifications can be used to confirm the jump. Xiaomi Hyper Island-style prompts are also supported.

</td>
<td width="50%">

### LSPosed / Shizuku Support
Supports a rootless Shizuku setup as well as Root / LSPosed clipboard change monitoring.

</td>
</tr>

<tr>
<td width="50%">

### Cloned Apps
When cloned app instances are detected, HyperCopy can show a dialog and let you choose which app instance to open.

</td>
<td width="50%">

### System Services
Adapts to the system link invocation service and supports quick configuration of the default link handling method.

</td>
</tr>
</table>

---

## Usage

1. Install HyperCopy.
2. Choose a monitoring method based on your environment: `LSPosed` or `Shizuku`.
3. Download common app rules from the Cloud Rules page, or manually add rules on the Rules page.
4. Copy a link, address, tracking number, or specified text.
5. When a rule matches, HyperCopy will either jump directly or show a confirmation notification based on your settings.

---

## Rule Capabilities

Rules are stored in `rules.json` under the app's private directory. Core fields include:

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

Supported template variables:

- `${p1}`, `${p2}`: Capture groups extracted by `parameterRegex` in order.
- `${r1}`, `${r1_1}`: Capture groups extracted by `extractionRegexes`.
- `${input}`: The full copied content.
- `${url:input}`: The first URL extracted from the full copied content.
- `${redirectUrl}`: The redirected URL resolved after jumping.
- `${raw:variableName}`: Inserts the parameter as-is without URL encoding.

---

## Build

Make sure Android Studio, JDK 17, and Android SDK are installed, then run:

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

## License

The license file is not available yet. Issues and PRs are welcome.

<div align="center">

Made with ❤️ for Android users

[![Star History](https://img.shields.io/github/stars/1812z/HyperCopy?style=social)](https://github.com/1812z/HyperCopy)

</div>
