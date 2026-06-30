package io.github.hypercopy.ui

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.hypercopy.Config

enum class AppLanguage(val value: String) {
    Chinese(Config.APP_LANGUAGE_ZH),
}

enum class AppColorMode(val value: String) {
    System(Config.COLOR_MODE_SYSTEM),
    Light(Config.COLOR_MODE_LIGHT),
    Dark(Config.COLOR_MODE_DARK),
}

enum class ClipboardMonitorMode(val value: String) {
    LSPosed(Config.CLIPBOARD_MONITOR_MODE_LSPOSED),
}

val LocalAppStrings = staticCompositionLocalOf { zhStrings }

fun appLanguageFromValue(value: String): AppLanguage = AppLanguage.Chinese

fun appColorModeFromValue(value: String): AppColorMode = when (value) {
    Config.COLOR_MODE_LIGHT -> AppColorMode.Light
    Config.COLOR_MODE_DARK -> AppColorMode.Dark
    else -> AppColorMode.System
}

fun clipboardMonitorModeFromValue(value: String): ClipboardMonitorMode = ClipboardMonitorMode.LSPosed

data class UiStrings(
    val home: String,
    val copy: String,
    val rules: String,
    val settings: String,
    val working: String,
    val notActive: String,
    val moduleConnected: String,
    val moduleDisconnected: String,
    val systemVersion: String,
    val appVersion: String,
    val androidVersion: String,
    val lsposedVersion: String,
    val buildDate: String,
    val deviceModel: String,
    val unknown: String,
    val workMode: String,
    val appearance: String,
    val language: String,
    val languageSummary: String,
    val languageChinese: String,
    val theme: String,
    val themeSummary: String,
    val colorMode: String,
    val colorModeSummary: String,
    val colorModeSystem: String,
    val colorModeLight: String,
    val colorModeDark: String,
    val softwareSettings: String,
    val clipboardMonitorMode: String,
    val clipboardMonitorModeSummary: String,
    val clipboardMonitorModeLSPosed: String,
    val logLevel: String,
    val logLevelSummary: String,
    val logOff: String,
    val logBasic: String,
    val logDebug: String,
    val checkUpdate: String,
    val checkUpdateSummary: String,
    val autoCheckUpdate: String,
    val autoCheckUpdateSummary: String,
    val hideDesktopIcon: String,
    val hideDesktopIconSummary: String,
    val about: String,
    val openHomePage: String,
    val openHomePageSummary: String,
)

val zhStrings = UiStrings(
    home = "主页",
    copy = "复制",
    rules = "规则",
    settings = "设置",
    working = "工作中",
    notActive = "未激活",
    moduleConnected = "模块服务已连接",
    moduleDisconnected = "模块服务未连接",
    systemVersion = "系统版本",
    appVersion = "应用版本",
    androidVersion = "Android 版本",
    lsposedVersion = "LSPosed 版本",
    buildDate = "构建日期",
    deviceModel = "设备型号",
    unknown = "未知",
    workMode = "工作方式",
    appearance = "外观",
    language = "语言",
    languageSummary = "当前仅支持中文",
    languageChinese = "中文",
    theme = "主题",
    themeSummary = "颜色模式",
    colorMode = "颜色模式",
    colorModeSummary = "控制应用浅色或深色显示",
    colorModeSystem = "跟随系统",
    colorModeLight = "亮色",
    colorModeDark = "暗色",
    softwareSettings = "软件设置",
    clipboardMonitorMode = "监听方案",
    clipboardMonitorModeSummary = "当前使用 LSPosed Hook 系统剪贴板服务",
    clipboardMonitorModeLSPosed = "LSPosed",
    logLevel = "日志级别",
    logLevelSummary = "控制模块输出日志的详细程度",
    logOff = "关闭",
    logBasic = "基础",
    logDebug = "调试",
    checkUpdate = "检查更新",
    checkUpdateSummary = "通过 GitHub Release 检查最新版本",
    autoCheckUpdate = "自动检查更新",
    autoCheckUpdateSummary = "启动时检查 GitHub Release 是否有新版本",
    hideDesktopIcon = "隐藏桌面图标",
    hideDesktopIconSummary = "开启后启动器中不显示应用图标，仍可从 LSPosed 模块设置打开",
    about = "关于",
    openHomePage = "打开主页",
    openHomePageSummary = "打开 HyperCopy 项目主页",
)
