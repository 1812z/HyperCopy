package io.github.hypercopy.clipboard.monitor

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.hypercopy.Config
import io.github.hypercopy.data.settings.SettingsRepository

object ClipboardMonitorController {
    fun startForCurrentMode(context: Context) {
        val appContext = context.applicationContext
        when (SettingsRepository(appContext).readClipboardMonitorMode()) {
            Config.CLIPBOARD_MONITOR_MODE_SHIZUKU -> startForegroundService(appContext)
            else -> stopForegroundService(appContext)
        }
    }

    fun onModeChanged(context: Context, mode: String) {
        val appContext = context.applicationContext
        if (mode == Config.CLIPBOARD_MONITOR_MODE_SHIZUKU) {
            startForegroundService(appContext)
        } else {
            stopForegroundService(appContext)
        }
    }

    private fun startForegroundService(context: Context) {
        ContextCompat.startForegroundService(context, Intent(context, ClipboardMonitorForegroundService::class.java))
    }

    private fun stopForegroundService(context: Context) {
        context.stopService(Intent(context, ClipboardMonitorForegroundService::class.java))
        ShizukuClipboardMonitor.stop()
    }
}
