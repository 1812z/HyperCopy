package io.github.hypercopy.clipboard

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.util.concurrent.TimeUnit

object RootActivityLauncher {
    private const val TAG = "HyperCopy"
    private const val TIMEOUT_SECONDS = 5L

    fun launch(intent: Intent): Boolean {
        val command = intent.toAmStartCommand() ?: return false
        return runCatching {
            Log.d(TAG, "root start activity: $command")
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                Log.d(TAG, "root start activity timeout")
                return false
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val success = process.exitValue() == 0
            if (!success) Log.d(TAG, "root start activity failed: $output")
            success
        }.getOrElse { throwable ->
            Log.d(TAG, "root start activity exception", throwable)
            false
        }
    }

    private fun Intent.toAmStartCommand(): String? {
        val args = mutableListOf("am", "start", "--user", "0")
        action?.takeIf { it.isNotBlank() }?.let { args += listOf("-a", it) }
        dataString?.takeIf { it.isNotBlank() }?.let { args += listOf("-d", it) }
        categories?.forEach { category -> args += listOf("-c", category) }
        component?.flattenToString()?.takeIf { it.isNotBlank() }?.let { args += listOf("-n", it) }
        if (component == null) {
            `package`?.takeIf { it.isNotBlank() }?.let { args += listOf("-p", it) }
        }
        extras?.keySet()?.forEach { key ->
            val value = extras?.get(key)
            if (value is String) args += listOf("--es", key, value)
        }
        return args.joinToString(" ") { it.shellQuote() }
    }

    fun intentToCommandPreview(intent: Intent): String? = intent.toAmStartCommand()

    private fun String.shellQuote(): String {
        return "'" + replace("'", "'\\''") + "'"
    }
}

fun Intent.withResolvedActivity(packageManager: android.content.pm.PackageManager): Intent {
    if (component != null) return this
    val resolved = resolveActivity(packageManager) ?: return this
    return Intent(this).setComponent(ComponentName(resolved.packageName, resolved.className))
}

fun String.toViewIntent(packageName: String = ""): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse(this)).apply {
        if (packageName.isNotBlank()) setPackage(packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
