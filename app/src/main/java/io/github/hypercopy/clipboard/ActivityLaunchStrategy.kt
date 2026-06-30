package io.github.hypercopy.clipboard

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import io.github.hypercopy.Config
import io.github.hypercopy.R
import io.github.hypercopy.data.SettingsRepository

object ActivityLaunchStrategy {
    private const val TAG = "HyperCopy"

    fun launch(context: Context, intent: Intent): Boolean {
        val resolvedIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).withResolvedActivity(context.packageManager)
        val launched = if (SettingsRepository(context.applicationContext).readClipboardMonitorMode() == Config.CLIPBOARD_MONITOR_MODE_SHIZUKU) {
            ShizukuActivityLauncher.launch(resolvedIntent)
        } else {
            RootActivityLauncher.launch(resolvedIntent) || launchNormally(context, resolvedIntent)
        }
        if (!launched) Toast.makeText(context, R.string.toast_open_target_failed, Toast.LENGTH_SHORT).show()
        return launched
    }

    private fun launchNormally(context: Context, intent: Intent): Boolean {
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrElse { throwable ->
            Log.w(TAG, "Failed to start activity", throwable)
            false
        }
    }
}
