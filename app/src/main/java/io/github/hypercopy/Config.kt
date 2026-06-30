package io.github.hypercopy

object Config {
    const val PREFS_NAME = "hypercopy_settings"
    const val KEY_LOG_LEVEL = "log_level"
    const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"
    const val KEY_APP_LANGUAGE = "app_language"
    const val KEY_COLOR_MODE = "color_mode"

    const val LOG_LEVEL_OFF = 0
    const val LOG_LEVEL_BASIC = 1
    const val LOG_LEVEL_DEBUG = 2

    const val APP_LANGUAGE_ZH = "zh"

    const val COLOR_MODE_SYSTEM = "system"
    const val COLOR_MODE_LIGHT = "light"
    const val COLOR_MODE_DARK = "dark"

    const val DEFAULT_LOG_LEVEL = LOG_LEVEL_BASIC
    const val DEFAULT_AUTO_CHECK_UPDATE = true
    const val DEFAULT_APP_LANGUAGE = APP_LANGUAGE_ZH
    const val DEFAULT_COLOR_MODE = COLOR_MODE_SYSTEM
}
