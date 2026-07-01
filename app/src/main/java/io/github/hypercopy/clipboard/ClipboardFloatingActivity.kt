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
        if (!consumeStartToken()) {
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
        if (!hasFocus) return
        when (intent.getStringExtra(ClipboardFocusRequester.EXTRA_ACTION)) {
            ClipboardFocusRequester.ACTION_CLEAR_CLIPBOARD -> clearClipboardAndFinish()
            else -> readClipboardAndFinish()
        }
    }

    private fun consumeStartToken(): Boolean {
        val token = intent.getStringExtra(ClipboardFocusRequester.EXTRA_START_TOKEN)
        return when (intent.getStringExtra(ClipboardFocusRequester.EXTRA_ACTION)) {
            ClipboardFocusRequester.ACTION_CLEAR_CLIPBOARD -> ClipboardFocusRequester.isPendingClearToken(token)
            else -> ClipboardFocusRequester.consumeToken(token)
        }
    }

    private fun readClipboardAndFinish() {
        if (handled) return
        handled = true
        val text = readPrimaryText()
        finishWithoutAnimation()
        if (!text.isNullOrBlank()) {
            Handler(Looper.getMainLooper()).postDelayed({
                ClipboardTextHandler.handle(applicationContext, text, intent.getStringExtra(ClipboardFocusRequester.EXTRA_SOURCE_PACKAGE).orEmpty())
            }, HANDLE_AFTER_FINISH_DELAY_MILLIS)
        }
    }

    private fun clearClipboardAndFinish() {
        if (handled) return
        handled = true
        val cleared = runCatching {
            val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("", ""))
        }.isSuccess
        ClipboardFocusRequester.consumeClearToken(
            intent.getStringExtra(ClipboardFocusRequester.EXTRA_START_TOKEN),
            cleared,
        )
        finishWithoutAnimation()
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
        const val HANDLE_AFTER_FINISH_DELAY_MILLIS = 120L
    }
}
