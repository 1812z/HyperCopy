package io.github.hypercopy

object Config {
    const val APPLICATION_ID = "io.github.hypercopy"

    const val PREFS_NAME = "hypercopy_settings"
    const val KEY_LOG_LEVEL = "log_level"
    const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"
    const val KEY_APP_LANGUAGE = "app_language"
    const val KEY_COLOR_MODE = "color_mode"
    const val KEY_CLIPBOARD_MONITOR_MODE = "clipboard_monitor_mode"

    const val ACTION_HANDLE_CLIPBOARD_TEXT = "io.github.hypercopy.action.HANDLE_CLIPBOARD_TEXT"
    const val EXTRA_CLIPBOARD_TEXT = "io.github.hypercopy.extra.CLIPBOARD_TEXT"
    const val EXTRA_CLIPBOARD_SOURCE = "io.github.hypercopy.extra.CLIPBOARD_SOURCE"

    const val LOG_LEVEL_OFF = 0
    const val LOG_LEVEL_BASIC = 1
    const val LOG_LEVEL_DEBUG = 2

    const val APP_LANGUAGE_ZH = "zh"

    const val COLOR_MODE_SYSTEM = "system"
    const val COLOR_MODE_LIGHT = "light"
    const val COLOR_MODE_DARK = "dark"

    const val CLIPBOARD_MONITOR_MODE_LSPOSED = "lsposed"
    const val CLIPBOARD_MONITOR_MODE_SHIZUKU = "shizuku"

    const val DEFAULT_LOG_LEVEL = LOG_LEVEL_BASIC
    const val DEFAULT_AUTO_CHECK_UPDATE = true
    const val DEFAULT_APP_LANGUAGE = APP_LANGUAGE_ZH
    const val DEFAULT_COLOR_MODE = COLOR_MODE_SYSTEM
    const val DEFAULT_CLIPBOARD_MONITOR_MODE = CLIPBOARD_MONITOR_MODE_LSPOSED

    const val CLIPBOARD_TEXT_MAX_LENGTH = 16_384
}
