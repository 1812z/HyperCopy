package io.github.hypercopy.ui

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
    Shizuku(Config.CLIPBOARD_MONITOR_MODE_SHIZUKU),
}

fun appLanguageFromValue(value: String): AppLanguage = AppLanguage.Chinese

fun appColorModeFromValue(value: String): AppColorMode = when (value) {
    Config.COLOR_MODE_LIGHT -> AppColorMode.Light
    Config.COLOR_MODE_DARK -> AppColorMode.Dark
    else -> AppColorMode.System
}

fun clipboardMonitorModeFromValue(value: String): ClipboardMonitorMode = when (value) {
    Config.CLIPBOARD_MONITOR_MODE_SHIZUKU -> ClipboardMonitorMode.Shizuku
    else -> ClipboardMonitorMode.LSPosed
}
