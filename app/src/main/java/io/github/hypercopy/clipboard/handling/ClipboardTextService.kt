package io.github.hypercopy.clipboard.handling

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import io.github.hypercopy.Config
import io.github.hypercopy.data.settings.SettingsRepository

class ClipboardTextService : Service() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Config.ACTION_HANDLE_CLIPBOARD_TEXT &&
            SettingsRepository(applicationContext).readClipboardMonitorMode() == Config.CLIPBOARD_MONITOR_MODE_LSPOSED
        ) {
            val text = intent.getStringExtra(Config.EXTRA_CLIPBOARD_TEXT)
            val source = intent.getStringExtra(Config.EXTRA_CLIPBOARD_SOURCE) ?: "unknown"
            if (text != null) ClipboardTextHandler.handle(this, text, source)
        }
        handler.postDelayed({ stopSelf(startId) }, KEEP_ALIVE_MILLIS)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val KEEP_ALIVE_MILLIS = 20_000L
    }
}
