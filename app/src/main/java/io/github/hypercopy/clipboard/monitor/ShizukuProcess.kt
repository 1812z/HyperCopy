package io.github.hypercopy.clipboard.monitor

import android.util.Log

object ShizukuProcess {
    private const val TAG = "HyperCopy"

    fun start(command: Array<String>): Process? {
        return runCatching {
            val method = Class.forName("rikka.shizuku.Shizuku").getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            )
            method.isAccessible = true
            method.invoke(null, command, null, null) as Process
        }.getOrElse { throwable ->
            Log.d(TAG, "Shizuku newProcess reflection failed", throwable)
            null
        }
    }
}
