package io.github.hypercopy.clipboard.jump

import android.content.Intent

sealed interface PendingJump {
    val title: String
    val packageName: String

    data class IntentJump(
        override val title: String,
        val intent: Intent,
        override val packageName: String = intent.`package` ?: intent.component?.packageName.orEmpty(),
    ) : PendingJump

    data class WebViewJump(
        override val title: String,
        val url: String,
        override val packageName: String,
    ) : PendingJump

    data class SystemLinkJump(
        override val title: String,
        val url: String,
        val userId: Int,
        override val packageName: String,
    ) : PendingJump
}
