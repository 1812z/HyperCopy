package io.github.hypercopy.clipboard

import android.content.Context
import io.github.hypercopy.HyperLog
import io.github.hypercopy.clipboard.monitor.ShizukuPermission
import io.github.hypercopy.clipboard.monitor.ShizukuProcess

object MiuiXmsfNetworkBlocker {
    private const val TAG = "HyperCopy"
    private const val XMSF_PACKAGE = "com.xiaomi.xmsf"
    private const val BINDER_COMMAND_CLASS = "io.github.hypercopy.clipboard.MiuiXmsfFirewallBinderCommand"
    private const val BLOCK_MILLIS = 100L
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

        try {
            setUidNetworkBlocked(context, uid, blocked = true)
            notify()
            Thread.sleep(BLOCK_MILLIS)
        } catch (throwable: Throwable) {
            HyperLog.d(TAG, "xmsf temporary network block failed", throwable)
            notify()
        } finally {
            runCatching { setUidNetworkBlocked(context, uid, blocked = false) }
                .onFailure { HyperLog.d(TAG, "xmsf network restore failed", it) }
        }
    }

    private fun setUidNetworkBlocked(context: Context, uid: Int, blocked: Boolean) {
        val process = ShizukuProcess.start(
            arrayOf(
                "app_process",
                "-Djava.class.path=${context.applicationInfo.sourceDir}",
                "/system/bin",
                BINDER_COMMAND_CLASS,
                uid.toString(),
                if (blocked) "block" else "restore",
            ),
        ) ?: error("Shizuku app_process unavailable")
        if (!waitForExit(process)) {
            runCatching { process.destroyForcibly() }
            error("timeout")
        }
        val output = runCatching { process.inputStream.bufferedReader().use { it.readText() } }.getOrDefault("")
        val exitCode = runCatching { process.exitValue() }.getOrDefault(-1)
        if (exitCode != 0) error("exit=$exitCode output=${output.take(300)}")
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
