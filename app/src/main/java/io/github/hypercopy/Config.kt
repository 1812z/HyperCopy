package io.github.hypercopy

object Config {
    const val APPLICATION_ID = "io.github.hypercopy"

    const val PREFS_NAME = "hypercopy_settings"
    const val KEY_LOG_LEVEL = "log_level"
    const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"
    const val KEY_HIDE_FROM_RECENTS = "hide_from_recents"
    const val KEY_APP_LANGUAGE = "app_language"
    const val KEY_COLOR_MODE = "color_mode"
    const val KEY_CLIPBOARD_MONITOR_MODE = "clipboard_monitor_mode"
    const val KEY_JUMP_NOTIFICATION_MODE = "jump_notification_mode"
    const val KEY_MIUI_ISLAND_BYPASS_RESTRICTION = "miui_island_bypass_restriction"
    const val KEY_APP_LIST_WORK_MODE = "app_list_work_mode"
    const val KEY_IGNORE_JUMP_APP = "ignore_jump_app"
    const val KEY_DETECT_CLONED_APP = "detect_cloned_app"
    const val KEY_SYSTEM_LINK_HANDLING = "system_link_handling"
    const val KEY_SYSTEM_LINK_CLEAR_CLIPBOARD_AFTER_JUMP = "system_link_clear_clipboard_after_jump"
    const val KEY_SYSTEM_LINK_USER_ID = "system_link_user_id"
    const val KEY_APP_LIST_PACKAGES = "app_list_packages"

    const val ACTION_HANDLE_CLIPBOARD_TEXT = "io.github.hypercopy.action.HANDLE_CLIPBOARD_TEXT"
    const val ACTION_CLEAR_CLIPBOARD = "io.github.hypercopy.action.CLEAR_CLIPBOARD"
    const val ACTION_CONFIRM_JUMP = "io.github.hypercopy.action.CONFIRM_JUMP"
    const val PERMISSION_CLEAR_CLIPBOARD = "io.github.hypercopy.permission.CLEAR_CLIPBOARD"
    const val EXTRA_CLIPBOARD_TEXT = "io.github.hypercopy.extra.CLIPBOARD_TEXT"
    const val EXTRA_CLIPBOARD_SOURCE = "io.github.hypercopy.extra.CLIPBOARD_SOURCE"
    const val EXTRA_PENDING_JUMP_ID = "io.github.hypercopy.extra.PENDING_JUMP_ID"
    const val EXTRA_PENDING_JUMP_USER_ID = "io.github.hypercopy.extra.PENDING_JUMP_USER_ID"

    const val LOG_LEVEL_OFF = 0
    const val LOG_LEVEL_BASIC = 1
    const val LOG_LEVEL_DEBUG = 2

    const val APP_LANGUAGE_SYSTEM = "system"
    const val APP_LANGUAGE_ZH = "zh"
    const val APP_LANGUAGE_EN = "en"

    const val COLOR_MODE_SYSTEM = "system"
    const val COLOR_MODE_LIGHT = "light"
    const val COLOR_MODE_DARK = "dark"

    const val CLIPBOARD_MONITOR_MODE_LSPOSED = "lsposed"
    const val CLIPBOARD_MONITOR_MODE_SHIZUKU = "shizuku"

    const val JUMP_NOTIFICATION_MODE_NONE = "none"
    const val JUMP_NOTIFICATION_MODE_NORMAL = "normal"
    const val JUMP_NOTIFICATION_MODE_LIVE = "live"
    const val JUMP_NOTIFICATION_MODE_MIUI_ISLAND = "miui_island"

    const val APP_LIST_WORK_MODE_WHITELIST = "whitelist"
    const val APP_LIST_WORK_MODE_BLACKLIST = "blacklist"

    const val DEFAULT_LOG_LEVEL = LOG_LEVEL_BASIC
    const val DEFAULT_AUTO_CHECK_UPDATE = true
    const val DEFAULT_HIDE_FROM_RECENTS = false
    const val DEFAULT_APP_LANGUAGE = APP_LANGUAGE_SYSTEM
    const val DEFAULT_COLOR_MODE = COLOR_MODE_SYSTEM
    const val DEFAULT_CLIPBOARD_MONITOR_MODE = CLIPBOARD_MONITOR_MODE_LSPOSED
    const val DEFAULT_JUMP_NOTIFICATION_MODE = JUMP_NOTIFICATION_MODE_NONE
    const val DEFAULT_MIUI_ISLAND_BYPASS_RESTRICTION = false
    const val DEFAULT_APP_LIST_WORK_MODE = APP_LIST_WORK_MODE_BLACKLIST
    const val DEFAULT_IGNORE_JUMP_APP = true
    const val DEFAULT_DETECT_CLONED_APP = true
    const val DEFAULT_SYSTEM_LINK_HANDLING = true
    const val DEFAULT_SYSTEM_LINK_CLEAR_CLIPBOARD_AFTER_JUMP = true
    const val DEFAULT_SYSTEM_LINK_USER_ID = 0

    const val KEY_CLOUD_SOURCE = "cloud_source"
    const val CLOUD_SOURCE_GITHUB = "github"
    const val CLOUD_SOURCE_ACCELERATED = "accelerated"
    const val DEFAULT_CLOUD_SOURCE = CLOUD_SOURCE_ACCELERATED

    const val CLIPBOARD_TEXT_MAX_LENGTH = 16_384
}
