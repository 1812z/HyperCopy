package io.github.hypercopy.ui

import io.github.hypercopy.Config

enum class AppLanguage(val value: String) {
    System(Config.APP_LANGUAGE_SYSTEM),
    Chinese(Config.APP_LANGUAGE_ZH),
    English(Config.APP_LANGUAGE_EN),
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

enum class JumpNotificationMode(val value: String) {
    None(Config.JUMP_NOTIFICATION_MODE_NONE),
    Normal(Config.JUMP_NOTIFICATION_MODE_NORMAL),
    Live(Config.JUMP_NOTIFICATION_MODE_LIVE),
    MiuiIsland(Config.JUMP_NOTIFICATION_MODE_MIUI_ISLAND),
}

fun appLanguageFromValue(value: String): AppLanguage = when (value) {
    Config.APP_LANGUAGE_ZH -> AppLanguage.Chinese
    Config.APP_LANGUAGE_EN -> AppLanguage.English
    else -> AppLanguage.System
}

fun appColorModeFromValue(value: String): AppColorMode = when (value) {
    Config.COLOR_MODE_LIGHT -> AppColorMode.Light
    Config.COLOR_MODE_DARK -> AppColorMode.Dark
    else -> AppColorMode.System
}

fun clipboardMonitorModeFromValue(value: String): ClipboardMonitorMode = when (value) {
    Config.CLIPBOARD_MONITOR_MODE_SHIZUKU -> ClipboardMonitorMode.Shizuku
    else -> ClipboardMonitorMode.LSPosed
}

fun jumpNotificationModeFromValue(value: String): JumpNotificationMode = when (value) {
    Config.JUMP_NOTIFICATION_MODE_NORMAL -> JumpNotificationMode.Normal
    Config.JUMP_NOTIFICATION_MODE_LIVE -> JumpNotificationMode.Live
    Config.JUMP_NOTIFICATION_MODE_MIUI_ISLAND -> JumpNotificationMode.MiuiIsland
    else -> JumpNotificationMode.None
}
