package io.github.hypercopy.clipboard.jump

import android.content.ClipData
import android.content.Context
import android.content.Intent
import io.github.hypercopy.HyperLog

/** Marks a clipboard write so the LSPosed system hook can invoke MIUI ContentExtension. */
object MiuiSystemCopy {
    private const val TAG = "HyperCopy"
    const val CLIP_LABEL = "HyperCopySystemCopy"

    fun copy(context: Context, jump: PendingJump): Boolean {
        val text = when (jump) {
            is PendingJump.IntentJump -> jump.intent.dataString
                ?: jump.intent.getStringExtra(Intent.EXTRA_TEXT)
            is PendingJump.WebViewJump -> jump.url
            is PendingJump.SystemLinkJump -> jump.url
        }?.trim().orEmpty()
        if (text.isEmpty()) {
            HyperLog.d(TAG, "system copy jump has no URL/text")
            return false
        }

        if (SystemCopyOverlayService.start(context, jump)) {
            HyperLog.d(TAG, "system copy overlay requested for ${jump.packageName}")
            return true
        }

        return write(context, CLIP_LABEL, text)
    }

    private fun write(context: Context, label: String, text: String): Boolean {
        return runCatching {
            val clipboard = context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE)
                as? android.content.ClipboardManager
                ?: return false
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            HyperLog.d(TAG, "clipboard replay marked for MIUI ContentExtension, label=$label, length=${text.length}")
            true
        }.getOrElse { throwable ->
            HyperLog.d(TAG, "system copy jump failed", throwable)
            false
        }
    }
}
