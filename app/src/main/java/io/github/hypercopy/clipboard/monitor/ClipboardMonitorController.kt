package io.github.hypercopy.clipboard.monitor

import android.content.Context
import io.github.hypercopy.Config
import io.github.hypercopy.data.SettingsRepository

object ClipboardMonitorController {
    fun startForCurrentMode(context: Context) {
        val appContext = context.applicationContext
        when (SettingsRepository(appContext).readClipboardMonitorMode()) {
            Config.CLIPBOARD_MONITOR_MODE_SHIZUKU -> ShizukuClipboardMonitor.start(appContext)
            else -> ShizukuClipboardMonitor.stop()
        }
    }

    fun onModeChanged(context: Context, mode: String) {
        val appContext = context.applicationContext
        if (mode == Config.CLIPBOARD_MONITOR_MODE_SHIZUKU) {
            ShizukuClipboardMonitor.start(appContext)
        } else {
            ShizukuClipboardMonitor.stop()
        }
    }
}
