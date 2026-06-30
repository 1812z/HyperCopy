package io.github.hypercopy.clipboard

import android.content.Intent
import android.util.Log
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
            Log.d(TAG, "Shizuku start activity: $command")
            val process = ShizukuProcess.start(arrayOf("sh", "-c", command)) ?: return false
            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                Log.d(TAG, "Shizuku start activity timeout")
                return false
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val success = process.exitValue() == 0
            if (!success) Log.d(TAG, "Shizuku start activity failed: $output")
            success
        }.getOrElse { throwable ->
            Log.d(TAG, "Shizuku start activity exception", throwable)
            false
        }
    }
}
