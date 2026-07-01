package io.github.hypercopy.clipboard

import android.content.Context
import android.content.Intent
import android.widget.Toast
import io.github.hypercopy.Config
import io.github.hypercopy.HyperLog
import io.github.hypercopy.R
import io.github.hypercopy.data.SettingsRepository

object ActivityLaunchStrategy {
    private const val TAG = "HyperCopy"

    fun launch(context: Context, intent: Intent): Boolean {
        val resolvedIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).withResolvedActivity(context.packageManager)
        val usesPrivilegedLauncher = SettingsRepository(context.applicationContext).readClipboardMonitorMode() == Config.CLIPBOARD_MONITOR_MODE_SHIZUKU
        val launched = if (usesPrivilegedLauncher) {
            ShizukuActivityLauncher.launch(resolvedIntent) || launchNormally(context, resolvedIntent)
        } else {
            RootActivityLauncher.launch(resolvedIntent) || launchNormally(context, resolvedIntent)
        }
        if (!launched && usesPrivilegedLauncher) HyperLog.d(TAG, "Privileged start returned failure; suppress toast to avoid false negatives")
        if (!launched && !usesPrivilegedLauncher) Toast.makeText(context, R.string.toast_open_target_failed, Toast.LENGTH_SHORT).show()
        return launched
    }

    private fun launchNormally(context: Context, intent: Intent): Boolean {
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrElse { throwable ->
            HyperLog.w(TAG, "Failed to start activity", throwable)
            false
        }
    }
}
