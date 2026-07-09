package io.github.hypercopy.clipboard

import android.content.Context
import io.github.hypercopy.HyperLog
import io.github.hypercopy.clipboard.monitor.ShizukuPermission
import io.github.hypercopy.clipboard.monitor.ShizukuProcess

object MiuiXmsfNetworkBlocker {
    private const val TAG = "HyperCopy"
    private const val XMSF_PACKAGE = "com.xiaomi.xmsf"
    private const val BINDER_COMMAND_CLASS = "io.github.hypercopy.clipboard.MiuiXmsfFirewallBinderCommand"
    private const val BLOCK_MILLIS = 80L
    private const val TIMEOUT_MILLIS = 1_500L

    fun notifyWithTemporaryBlock(context: Context, notify: () -> Unit) {
        if (!ShizukuPermission.isGranted()) {
            notify()
            return
        }

        val uid = runCatching { context.packageManager.getPackageUid(XMSF_PACKAGE, 0) }.getOrNull()
        if (uid == null) {
            notify()
            return
        }

        var notified = false
        try {
            withTemporaryBlockSession(context, uid) {
                notify()
                notified = true
                Thread.sleep(BLOCK_MILLIS)
            }
        } catch (throwable: Throwable) {
            HyperLog.d(TAG, "xmsf temporary network block failed", throwable)
            if (!notified) notify()
        }
    }

    private fun withTemporaryBlockSession(context: Context, uid: Int, onBlocked: () -> Unit) {
        val process = startBinderProcess(context, uid, "session")
        val reader = process.inputStream.bufferedReader()
        var ready = false
        var restoreRequested = false
        try {
            ready = waitForReady(process, reader)
            if (!ready) error("xmsf block session did not become ready")
            onBlocked()
            process.outputStream.write('\n'.code)
            process.outputStream.flush()
            restoreRequested = true
            if (!waitForExit(process)) {
                runCatching { process.destroyForcibly() }
                error("timeout")
            }
            val exitCode = runCatching { process.exitValue() }.getOrDefault(-1)
            if (exitCode != 0) error("exit=$exitCode")
        } finally {
            if (ready && !restoreRequested) {
                runCatching {
                    process.outputStream.write('\n'.code)
                    process.outputStream.flush()
                    waitForExit(process)
                }
            }
            runCatching { reader.close() }
            runCatching { process.outputStream.close() }
            if (runCatching { process.exitValue(); false }.getOrDefault(true)) {
                runCatching { process.destroyForcibly() }
            }
        }
    }

    private fun startBinderProcess(context: Context, uid: Int, action: String): Process {
        return ShizukuProcess.start(
            arrayOf(
                "app_process",
                "-Djava.class.path=${context.applicationInfo.sourceDir}",
                "/system/bin",
                BINDER_COMMAND_CLASS,
                uid.toString(),
                action,
            ),
        ) ?: error("Shizuku app_process unavailable")
    }

    private fun waitForReady(process: Process, reader: java.io.BufferedReader): Boolean {
        val deadline = System.currentTimeMillis() + TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            val exited = runCatching {
                process.exitValue()
                true
            }.getOrDefault(false)
            if (exited) return false

            if (reader.ready() && reader.readLine() == "READY") return true
            runCatching { Thread.sleep(20L) }
        }
        return false
    }

    private fun waitForExit(process: Process): Boolean {
        val deadline = System.currentTimeMillis() + TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            if (runCatching { process.exitValue(); true }.getOrDefault(false)) return true
            runCatching { Thread.sleep(20L) }
        }
        return false
    }
}
