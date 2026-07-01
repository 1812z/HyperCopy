package io.github.hypercopy.clipboard.monitor

import android.util.Log
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
                Log.d(TAG, "root permission check timeout")
                return false
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val success = process.exitValue() == 0
            if (!success) Log.d(TAG, "root permission check failed: $output")
            success
        }.getOrElse { throwable ->
            Log.d(TAG, "root permission check exception", throwable)
            false
        }
    }
}
