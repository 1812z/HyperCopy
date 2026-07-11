package io.github.hypercopy.clipboard.handling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.hypercopy.Config
import io.github.hypercopy.data.settings.SettingsRepository

class ClipboardTextReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Config.ACTION_HANDLE_CLIPBOARD_TEXT) return
        if (SettingsRepository(context.applicationContext).readClipboardMonitorMode() != Config.CLIPBOARD_MONITOR_MODE_LSPOSED) return
        val text = intent.getStringExtra(Config.EXTRA_CLIPBOARD_TEXT) ?: return
        val source = intent.getStringExtra(Config.EXTRA_CLIPBOARD_SOURCE) ?: "unknown"
        ClipboardTextHandler.handle(context, text, source)
    }
}
