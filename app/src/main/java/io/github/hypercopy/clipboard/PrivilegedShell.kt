package io.github.hypercopy.clipboard

import io.github.hypercopy.Config
import io.github.hypercopy.HyperLog
import io.github.hypercopy.clipboard.monitor.ShizukuPermission
import io.github.hypercopy.clipboard.monitor.ShizukuProcess
import io.github.hypercopy.data.settings.SettingsRepository

data class ShellResult(val exitCode: Int, val output: String)

object PrivilegedShell {
    private const val TAG = "HyperCopy"
    private const val TIMEOUT_SECONDS = 8L

    fun run(settingsRepository: SettingsRepository, command: String): ShellResult {
        return runCatching {
            val useShizuku = settingsRepository.readClipboardMonitorMode() == Config.CLIPBOARD_MONITOR_MODE_SHIZUKU && ShizukuPermission.isGranted()
            HyperLog.d(TAG, "privileged shell ${if (useShizuku) "shizuku" else "root"}: ${command.redactedShellCommand()}")
            val process = if (useShizuku) {
                ShizukuProcess.start(arrayOf("sh", "-c", "$command 2>&1"))
            } else {
                ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            } ?: return ShellResult(-1, "no privileged shell")

            val finished = waitForExit(process)
            if (!finished) {
                runCatching { process.destroyForcibly() }
                HyperLog.d(TAG, "privileged shell timeout: ${command.redactedShellCommand()}")
                return ShellResult(-1, "timeout")
            }

            val output = runCatching { process.inputStream.bufferedReader().use { it.readText() } }
                .getOrElse { throwable ->
                    HyperLog.d(TAG, "privileged shell read stdout failed", throwable)
                    ""
                }
            val exitCode = runCatching { process.exitValue() }
                .getOrElse { throwable ->
                    HyperLog.d(TAG, "privileged shell exitValue failed", throwable)
                    -1
                }
            if (exitCode != 0) HyperLog.d(TAG, "privileged shell failed code=$exitCode output=${output.take(300)}")
            ShellResult(exitCode, output)
        }.getOrElse { throwable ->
            HyperLog.d(TAG, "privileged shell exception: ${command.redactedShellCommand()}", throwable)
            ShellResult(-1, throwable.message.orEmpty())
        }
    }

    private fun String.redactedShellCommand(): String {
        return replace(Regex("(-d\\s+)'[^']*'")) { match -> "${match.groupValues[1]}'<redacted>'" }
            .replace(Regex("(--es\\s+'[^']+'\\s+)'[^']*'")) { match -> "${match.groupValues[1]}'<redacted>'" }
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
