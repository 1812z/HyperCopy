package io.github.hypercopy.clipboard

import android.content.Intent

sealed interface PendingJump {
    val title: String

    data class IntentJump(
        override val title: String,
        val intent: Intent,
    ) : PendingJump

    data class WebViewJump(
        override val title: String,
        val url: String,
        val packageName: String,
    ) : PendingJump
}
