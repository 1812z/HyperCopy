package io.github.hypercopy.clipboard.monitor

import io.github.hypercopy.HyperLog
import java.util.concurrent.TimeUnit

object RootPermission {
    private const val TAG = "HyperCopy"
    private const val TIMEOUT_SECONDS = 5L

    fun isGranted(): Boolean = request()

    fun request(): Boolean {
        return runCatching {
            val process = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                HyperLog.d(TAG, "root permission check timeout")
                return false
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val success = process.exitValue() == 0
            if (!success) HyperLog.d(TAG, "root permission check failed: $output")
            success
        }.getOrElse { throwable ->
            HyperLog.d(TAG, "root permission check exception", throwable)
            false
        }
    }
}
