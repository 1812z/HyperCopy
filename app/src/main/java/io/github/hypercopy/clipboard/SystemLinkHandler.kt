package io.github.hypercopy.clipboard

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import io.github.hypercopy.HyperLog
import io.github.hypercopy.R
import io.github.hypercopy.data.rules.extractFirstInputUrl
import io.github.hypercopy.data.settings.SettingsRepository
import io.github.hypercopy.data.systemlink.SystemLinkRepository

object SystemLinkHandler {
    private const val TAG = "HyperCopy"
    private const val GENERIC_WEB_URL = "https://www.example.com/"

    fun createJump(context: Context, input: String): PendingJump.SystemLinkJump? {
        val url = extractFirstInputUrl(input) ?: return null
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        if (!uri.scheme.equals("http", true) && !uri.scheme.equals("https", true)) return null

        val packageManager = context.packageManager
        val intent = systemViewIntent(uri)
        val appSpecificPackages = queryPackages(packageManager, intent) - genericWebPackages(packageManager) - context.packageName
        if (appSpecificPackages.isEmpty()) {
            HyperLog.d(TAG, "system link skipped, no app-specific handler: $url")
            return null
        }

        val resolvedPackage = intent.resolveActivity(packageManager)?.packageName.orEmpty()
        val packageName = resolvedPackage.takeIf { it in appSpecificPackages } ?: appSpecificPackages.first()
        val userId = SettingsRepository(context.applicationContext).readSystemLinkUserId()
        return PendingJump.SystemLinkJump(
            title = context.getString(R.string.rule_system_link_title),
            url = url,
            userId = userId,
            packageName = packageName,
        )
    }

    private fun systemViewIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun genericWebPackages(packageManager: PackageManager): Set<String> {
        return queryPackages(packageManager, systemViewIntent(Uri.parse(GENERIC_WEB_URL)))
    }

    private fun queryPackages(packageManager: PackageManager, intent: Intent): Set<String> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.mapNotNull { it.activityInfo?.packageName }.toSet()
    }
}
