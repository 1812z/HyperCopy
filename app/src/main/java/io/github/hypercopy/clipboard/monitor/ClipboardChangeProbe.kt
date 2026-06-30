package io.github.hypercopy.clipboard.monitor

import android.content.ClipboardManager
import android.content.Context
import android.util.Log

class ClipboardChangeProbe(context: Context) {
    private val appContext = context.applicationContext
    private val clipboardManager = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        Log.d(TAG, "clipboard change probe notified")
        runCatching { clipboardManager.primaryClip }
    }

    fun start() {
        clipboardManager.addPrimaryClipChangedListener(listener)
    }

    fun stop() {
        clipboardManager.removePrimaryClipChangedListener(listener)
    }

    private companion object {
        const val TAG = "HyperCopy"
    }
}
