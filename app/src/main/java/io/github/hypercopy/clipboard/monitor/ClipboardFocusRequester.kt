package io.github.hypercopy.clipboard.monitor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import io.github.hypercopy.HyperLog
import io.github.hypercopy.clipboard.ClipboardFloatingActivity
import io.github.hypercopy.clipboard.IntentAmStartCommand.shellQuote
import java.util.UUID

object ClipboardFocusRequester {
    private const val TAG = "HyperCopy"
    private const val REQUEST_DEBOUNCE_MILLIS = 800L
    private const val EXTRA_START_TOKEN = "io.github.hypercopy.extra.FLOATING_START_TOKEN"
    private const val EXTRA_SOURCE_PACKAGE = "io.github.hypercopy.extra.CLIPBOARD_SOURCE_PACKAGE"

    private var lastRequestAt = 0L
    private var pendingToken: String? = null

    fun request(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastRequestAt < REQUEST_DEBOUNCE_MILLIS) return
        lastRequestAt = now
        val token = UUID.randomUUID().toString()
        pendingToken = token
        val sourcePackage = foregroundPackageName(context)
        if (ShizukuPermission.isGranted() && startByShizuku(context, token, sourcePackage)) return
        runCatching { context.startActivity(floatingActivityIntent(context, token, sourcePackage)) }
            .onFailure { HyperLog.d(TAG, "start clipboard floating activity failed", it) }
    }

    fun consumeToken(token: String?): Boolean {
        val expected = pendingToken ?: return false
        if (token != expected) return false
        pendingToken = null
        return true
    }

    private fun startByShizuku(context: Context, token: String, sourcePackage: String): Boolean {
        val component = ComponentName(context.packageName, ClipboardFloatingActivity::class.java.name).flattenToString()
        val commandParts = mutableListOf(
            "am",
            "start",
            "--user",
            "0",
            "-n",
            component,
            "--es",
            EXTRA_START_TOKEN,
            token,
        )
        if (sourcePackage.isNotBlank()) {
            commandParts += listOf("--es", EXTRA_SOURCE_PACKAGE, sourcePackage)
        }
        commandParts += listOf(
            "-f",
            Intent.FLAG_ACTIVITY_NEW_TASK.toString(),
        )
        val command = commandParts.joinToString(" ") { it.shellQuote() }
        return runCatching {
            HyperLog.d(TAG, "Shizuku start clipboard floating activity")
            val process = ShizukuProcess.start(arrayOf("sh", "-c", command)) ?: return false
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val output = process.inputStream.bufferedReader().use { it.readText() }
                HyperLog.d(TAG, "Shizuku start clipboard floating activity failed: $output")
            }
            exitCode == 0
        }.getOrElse { throwable ->
            HyperLog.d(TAG, "Shizuku start clipboard floating activity exception", throwable)
            false
        }
    }

    private fun floatingActivityIntent(context: Context, token: String, sourcePackage: String): Intent {
        return Intent(context, ClipboardFloatingActivity::class.java)
            .putExtra(EXTRA_START_TOKEN, token)
            .putExtra(EXTRA_SOURCE_PACKAGE, sourcePackage)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun foregroundPackageName(context: Context): String {
        if (!ShizukuPermission.isGranted()) return ""
        return runCatching {
            val process = ShizukuProcess.start(arrayOf("sh", "-c", "dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'"))
                ?: return ""
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            Regex("[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+").findAll(output)
                .map { it.value }
                .firstOrNull { it != context.packageName } ?: ""
        }.getOrElse { throwable ->
            HyperLog.d(TAG, "read foreground package failed", throwable)
            ""
        }
    }
}
