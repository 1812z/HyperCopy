package io.github.hypercopy.clipboard

import android.content.Intent
import io.github.hypercopy.HyperLog
import io.github.hypercopy.clipboard.monitor.ShizukuPermission
import io.github.hypercopy.clipboard.monitor.ShizukuProcess
import java.util.concurrent.TimeUnit

object ShizukuActivityLauncher {
    private const val TAG = "HyperCopy"
    private const val TIMEOUT_SECONDS = 5L

    fun launch(intent: Intent): Boolean {
        if (!ShizukuPermission.isGranted()) return false
        val command = IntentAmStartCommand.build(intent)
        return runCatching {
            HyperLog.d(TAG, "Shizuku start activity: $command")
            val process = ShizukuProcess.start(arrayOf("sh", "-c", command)) ?: return false
            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                HyperLog.d(TAG, "Shizuku start activity timeout")
                return false
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val success = process.exitValue() == 0 || output.indicatesActivityStarted()
            if (!success) HyperLog.d(TAG, "Shizuku start activity failed: $output")
            success
        }.getOrElse { throwable ->
            HyperLog.d(TAG, "Shizuku start activity exception", throwable)
            false
        }
    }
}
