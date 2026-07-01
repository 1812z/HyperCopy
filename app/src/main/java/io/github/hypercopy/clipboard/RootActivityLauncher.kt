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
        val command = IntentAmStartCommand.build(intent)
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
            val success = process.exitValue() == 0 || output.indicatesActivityStarted()
            if (!success) Log.d(TAG, "root start activity failed: $output")
            success
        }.getOrElse { throwable ->
            Log.d(TAG, "root start activity exception", throwable)
            false
        }
    }

    fun intentToCommandPreview(intent: Intent): String = IntentAmStartCommand.build(intent)
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

internal fun String.indicatesActivityStarted(): Boolean {
    return contains("Starting:", ignoreCase = true) || contains("Warning: Activity not started", ignoreCase = true)
}
