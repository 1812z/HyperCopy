package io.github.hypercopy.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit
import io.github.hypercopy.Config

class SettingsRepository(private val context: Context) {
    fun readLogLevel(): Int {
        return preferences().getInt(Config.KEY_LOG_LEVEL, Config.DEFAULT_LOG_LEVEL)
    }

    fun persistLogLevel(value: Int) {
        preferences().edit(commit = true) { putInt(Config.KEY_LOG_LEVEL, value) }
    }

    fun readAutoCheckUpdate(): Boolean {
        return preferences().getBoolean(Config.KEY_AUTO_CHECK_UPDATE, Config.DEFAULT_AUTO_CHECK_UPDATE)
    }

    fun persistAutoCheckUpdate(value: Boolean) {
        preferences().edit(commit = true) { putBoolean(Config.KEY_AUTO_CHECK_UPDATE, value) }
    }

    fun readAppLanguage(): String {
        return preferences().getString(Config.KEY_APP_LANGUAGE, Config.DEFAULT_APP_LANGUAGE) ?: Config.DEFAULT_APP_LANGUAGE
    }

    fun persistAppLanguage(value: String) {
        preferences().edit(commit = true) { putString(Config.KEY_APP_LANGUAGE, value) }
    }

    fun readColorMode(): String {
        return preferences().getString(Config.KEY_COLOR_MODE, Config.DEFAULT_COLOR_MODE) ?: Config.DEFAULT_COLOR_MODE
    }

    fun persistColorMode(value: String) {
        preferences().edit(commit = true) { putString(Config.KEY_COLOR_MODE, value) }
    }

    fun readClipboardMonitorMode(): String {
        return preferences().getString(
            Config.KEY_CLIPBOARD_MONITOR_MODE,
            Config.DEFAULT_CLIPBOARD_MONITOR_MODE,
        ) ?: Config.DEFAULT_CLIPBOARD_MONITOR_MODE
    }

    fun persistClipboardMonitorMode(value: String) {
        preferences().edit(commit = true) { putString(Config.KEY_CLIPBOARD_MONITOR_MODE, value) }
    }

    fun readJumpNotificationMode(): String {
        return preferences().getString(
            Config.KEY_JUMP_NOTIFICATION_MODE,
            Config.DEFAULT_JUMP_NOTIFICATION_MODE,
        ) ?: Config.DEFAULT_JUMP_NOTIFICATION_MODE
    }

    fun persistJumpNotificationMode(value: String) {
        preferences().edit(commit = true) { putString(Config.KEY_JUMP_NOTIFICATION_MODE, value) }
    }

    fun readAppListWorkMode(): String {
        return preferences().getString(
            Config.KEY_APP_LIST_WORK_MODE,
            Config.DEFAULT_APP_LIST_WORK_MODE,
        ) ?: Config.DEFAULT_APP_LIST_WORK_MODE
    }

    fun persistAppListWorkMode(value: String) {
        preferences().edit(commit = true) { putString(Config.KEY_APP_LIST_WORK_MODE, value) }
    }

    fun readIgnoreJumpApp(): Boolean {
        return preferences().getBoolean(Config.KEY_IGNORE_JUMP_APP, Config.DEFAULT_IGNORE_JUMP_APP)
    }

    fun persistIgnoreJumpApp(value: Boolean) {
        preferences().edit(commit = true) { putBoolean(Config.KEY_IGNORE_JUMP_APP, value) }
    }

    fun readDetectClonedApp(): Boolean {
        return preferences().getBoolean(Config.KEY_DETECT_CLONED_APP, Config.DEFAULT_DETECT_CLONED_APP)
    }

    fun persistDetectClonedApp(value: Boolean) {
        preferences().edit(commit = true) { putBoolean(Config.KEY_DETECT_CLONED_APP, value) }
    }

    fun readSystemLinkHandling(): Boolean {
        return preferences().getBoolean(Config.KEY_SYSTEM_LINK_HANDLING, Config.DEFAULT_SYSTEM_LINK_HANDLING)
    }

    fun persistSystemLinkHandling(value: Boolean) {
        preferences().edit(commit = true) { putBoolean(Config.KEY_SYSTEM_LINK_HANDLING, value) }
    }

    fun readSystemLinkUserId(): Int {
        return preferences().getInt(Config.KEY_SYSTEM_LINK_USER_ID, Config.DEFAULT_SYSTEM_LINK_USER_ID)
    }

    fun persistSystemLinkUserId(value: Int) {
        preferences().edit(commit = true) { putInt(Config.KEY_SYSTEM_LINK_USER_ID, value) }
    }

    fun readAppListPackages(): Set<String> {
        return preferences().getStringSet(Config.KEY_APP_LIST_PACKAGES, emptySet()).orEmpty()
    }

    fun persistAppListPackages(value: Set<String>) {
        preferences().edit(commit = true) { putStringSet(Config.KEY_APP_LIST_PACKAGES, value) }
    }

    fun readDesktopIconHidden(): Boolean {
        return context.packageManager.getComponentEnabledSetting(desktopIconComponent()) ==
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    fun persistDesktopIconHidden(value: Boolean) {
        val state = if (value) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        context.packageManager.setComponentEnabledSetting(
            desktopIconComponent(),
            state,
            PackageManager.DONT_KILL_APP,
        )
    }

    fun readCloudSource(): String {
        return preferences().getString(Config.KEY_CLOUD_SOURCE, Config.DEFAULT_CLOUD_SOURCE)
            ?: Config.DEFAULT_CLOUD_SOURCE
    }

    fun persistCloudSource(value: String) {
        preferences().edit(commit = true) { putString(Config.KEY_CLOUD_SOURCE, value) }
    }

    private fun desktopIconComponent() = ComponentName(context.packageName, DESKTOP_ICON_ALIAS)

    private fun preferences() = context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val DESKTOP_ICON_ALIAS = "io.github.hypercopy.ui.MainActivityAlias"
    }
}
