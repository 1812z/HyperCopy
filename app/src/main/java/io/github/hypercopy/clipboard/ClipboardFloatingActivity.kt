package io.github.hypercopy.clipboard

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import io.github.hypercopy.Config
import io.github.hypercopy.clipboard.monitor.ClipboardFocusRequester
import io.github.hypercopy.data.SettingsRepository

class ClipboardFloatingActivity : Activity() {
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ClipboardFocusRequester.consumeToken(intent.getStringExtra(EXTRA_START_TOKEN))) {
            finish()
            return
        }
        if (SettingsRepository(applicationContext).readClipboardMonitorMode() != Config.CLIPBOARD_MONITOR_MODE_SHIZUKU) {
            finish()
            return
        }
        window.setBackgroundDrawableResource(android.R.color.transparent)
        val params = window.attributes
        params.dimAmount = 0f
        params.flags = params.flags or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        window.attributes = params
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) readClipboardAndFinish()
    }

    private fun readClipboardAndFinish() {
        if (handled) return
        handled = true
        val text = readPrimaryText()
        finishWithoutAnimation()
        if (!text.isNullOrBlank()) {
            Handler(Looper.getMainLooper()).postDelayed({
                ClipboardTextHandler.handle(applicationContext, text, "shizuku")
            }, HANDLE_AFTER_FINISH_DELAY_MILLIS)
        }
    }

    private fun finishWithoutAnimation() {
        overridePendingTransition(0, 0)
        moveTaskToBack(true)
        finishAndRemoveTask()
        overridePendingTransition(0, 0)
    }

    private fun readPrimaryText(): String? {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = manager.primaryClip ?: return null
        return clip.firstTextItem()?.coerceToText(this)?.toString()
    }

    private fun ClipData.firstTextItem(): ClipData.Item? {
        if (itemCount <= 0) return null
        return getItemAt(0)
    }

    private companion object {
        const val EXTRA_START_TOKEN = "io.github.hypercopy.extra.FLOATING_START_TOKEN"
        const val HANDLE_AFTER_FINISH_DELAY_MILLIS = 120L
    }
}
