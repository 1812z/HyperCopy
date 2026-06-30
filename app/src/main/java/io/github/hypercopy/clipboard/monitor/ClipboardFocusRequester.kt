package io.github.hypercopy.clipboard.monitor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.hypercopy.clipboard.ClipboardFloatingActivity
import io.github.hypercopy.clipboard.IntentAmStartCommand.shellQuote
import java.util.UUID

object ClipboardFocusRequester {
    private const val TAG = "HyperCopy"
    private const val REQUEST_DEBOUNCE_MILLIS = 800L
    private const val EXTRA_START_TOKEN = "io.github.hypercopy.extra.FLOATING_START_TOKEN"

    private var lastRequestAt = 0L
    private var pendingToken: String? = null

    fun request(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastRequestAt < REQUEST_DEBOUNCE_MILLIS) return
        lastRequestAt = now
        val token = UUID.randomUUID().toString()
        pendingToken = token
        if (ShizukuPermission.isGranted() && startByShizuku(context, token)) return
        runCatching { context.startActivity(floatingActivityIntent(context, token)) }
            .onFailure { Log.d(TAG, "start clipboard floating activity failed", it) }
    }

    fun consumeToken(token: String?): Boolean {
        val expected = pendingToken ?: return false
        if (token != expected) return false
        pendingToken = null
        return true
    }

    private fun startByShizuku(context: Context, token: String): Boolean {
        val component = ComponentName(context.packageName, ClipboardFloatingActivity::class.java.name).flattenToString()
        val command = listOf(
            "am",
            "start",
            "--user",
            "0",
            "-n",
            component,
            "--es",
            EXTRA_START_TOKEN,
            token,
            "-f",
            Intent.FLAG_ACTIVITY_NEW_TASK.toString(),
        ).joinToString(" ") { it.shellQuote() }
        return runCatching {
            Log.d(TAG, "Shizuku start clipboard floating activity")
            val process = ShizukuProcess.start(arrayOf("sh", "-c", command)) ?: return false
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val output = process.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Shizuku start clipboard floating activity failed: $output")
            }
            exitCode == 0
        }.getOrElse { throwable ->
            Log.d(TAG, "Shizuku start clipboard floating activity exception", throwable)
            false
        }
    }

    private fun floatingActivityIntent(context: Context, token: String): Intent {
        return Intent(context, ClipboardFloatingActivity::class.java)
            .putExtra(EXTRA_START_TOKEN, token)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
