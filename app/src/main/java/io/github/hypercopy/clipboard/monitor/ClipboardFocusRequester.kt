package io.github.hypercopy.clipboard.monitor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import io.github.hypercopy.HyperLog
import io.github.hypercopy.clipboard.handling.ClipboardFloatingActivity
import io.github.hypercopy.clipboard.privileged.IntentAmStartCommand
import java.util.UUID

object ClipboardFocusRequester {
    private const val TAG = "HyperCopy"
    private const val REQUEST_DEBOUNCE_MILLIS = 800L
    private const val SHIZUKU_COMMAND_TIMEOUT_MILLIS = 3_000L

    private var lastRequestAt = 0L
    private var pendingToken: String? = null
    private var pendingClearToken: String? = null
    private var pendingClearCallback: ((Boolean) -> Unit)? = null

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

    fun requestClear(context: Context, onComplete: (Boolean) -> Unit): String? {
        if (!ShizukuPermission.isGranted()) return null
        val token = UUID.randomUUID().toString()
        pendingClearToken = token
        pendingClearCallback = onComplete
        return if (startByShizuku(context, token, "", ACTION_CLEAR_CLIPBOARD)) {
            token
        } else {
            cancelClearToken(token)
            null
        }
    }

    fun consumeClearToken(token: String?, cleared: Boolean): Boolean {
        val expected = pendingClearToken ?: return false
        if (token != expected) return false
        pendingClearToken = null
        pendingClearCallback?.invoke(cleared)
        pendingClearCallback = null
        return true
    }

    fun cancelClearToken(token: String?) {
        if (token == null || token != pendingClearToken) return
        pendingClearToken = null
        pendingClearCallback = null
    }

    fun isPendingClearToken(token: String?): Boolean {
        return token != null && token == pendingClearToken
    }

    private fun startByShizuku(context: Context, token: String, sourcePackage: String, action: String = ACTION_READ_CLIPBOARD): Boolean {
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
            "--es",
            EXTRA_ACTION,
            action,
        )
        if (sourcePackage.isNotBlank()) {
            commandParts += listOf("--es", EXTRA_SOURCE_PACKAGE, sourcePackage)
        }
        commandParts += listOf(
            "-f",
            Intent.FLAG_ACTIVITY_NEW_TASK.toString(),
        )
        val command = commandParts.joinToString(" ") { IntentAmStartCommand.shellQuote(it) }
        return runCatching {
            HyperLog.d(TAG, "Shizuku start clipboard floating activity")
            val process = ShizukuProcess.start(arrayOf("sh", "-c", command)) ?: return false
            if (!waitForExit(process)) {
                process.destroyForcibly()
                HyperLog.d(TAG, "Shizuku start clipboard floating activity timeout")
                return false
            }
            val exitCode = process.exitValue()
            if (exitCode != 0) {
                val output = process.inputStream.bufferedReader().use { it.readText() }
                HyperLog.d(TAG, "Shizuku start clipboard floating activity failed: ${output.take(300)}")
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
            .putExtra(EXTRA_ACTION, ACTION_READ_CLIPBOARD)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun foregroundPackageName(context: Context): String {
        if (!ShizukuPermission.isGranted()) return ""
        return runCatching {
            val process = ShizukuProcess.start(arrayOf("sh", "-c", "dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'"))
                ?: return ""
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (!waitForExit(process)) process.destroyForcibly()
            Regex("[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+").findAll(output)
                .map { it.value }
                .firstOrNull { it != context.packageName } ?: ""
        }.getOrElse { throwable ->
            HyperLog.d(TAG, "read foreground package failed", throwable)
            ""
        }
    }

    private fun waitForExit(process: Process): Boolean {
        val deadline = System.currentTimeMillis() + SHIZUKU_COMMAND_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            val exited = runCatching {
                process.exitValue()
                true
            }.getOrDefault(false)
            if (exited) return true
            runCatching { Thread.sleep(50L) }
        }
        return false
    }

    const val EXTRA_START_TOKEN = "io.github.hypercopy.extra.FLOATING_START_TOKEN"
    const val EXTRA_SOURCE_PACKAGE = "io.github.hypercopy.extra.CLIPBOARD_SOURCE_PACKAGE"
    const val EXTRA_ACTION = "io.github.hypercopy.extra.FLOATING_ACTION"
    const val ACTION_READ_CLIPBOARD = "read_clipboard"
    const val ACTION_CLEAR_CLIPBOARD = "clear_clipboard"
}
