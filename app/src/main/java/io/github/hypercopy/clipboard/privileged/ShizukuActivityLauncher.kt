package io.github.hypercopy.clipboard.privileged

import android.content.Intent
import io.github.hypercopy.HyperLog
import io.github.hypercopy.clipboard.monitor.ShizukuPermission
import io.github.hypercopy.clipboard.monitor.ShizukuProcess

object ShizukuActivityLauncher {
    private const val TAG = "HyperCopy"
    private const val TIMEOUT_SECONDS = 5L

    fun launch(intent: Intent, userId: Int = 0): Boolean {
        if (!ShizukuPermission.isGranted()) return false
        val command = IntentAmStartCommand.build(intent, userId)
        return runCatching {
            HyperLog.d(TAG, "Shizuku start activity")
            val process = ShizukuProcess.start(arrayOf("sh", "-c", command)) ?: return false
            val finished = waitForExit(process)
            if (!finished) {
                process.destroyForcibly()
                HyperLog.d(TAG, "Shizuku start activity timeout")
                return false
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val success = process.exitValue() == 0 || output.indicatesActivityStarted()
            if (!success) HyperLog.d(TAG, "Shizuku start activity failed: ${output.take(300)}")
            success
        }.getOrElse { throwable ->
            HyperLog.d(TAG, "Shizuku start activity exception", throwable)
            false
        }
    }

    private fun waitForExit(process: Process): Boolean {
        val deadline = System.currentTimeMillis() + TIMEOUT_SECONDS * 1000L
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
}
