package io.github.hypercopy.clipboard

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import io.github.hypercopy.Config
import io.github.hypercopy.R
import io.github.hypercopy.data.RuleActionMode
import io.github.hypercopy.data.RuleConfig
import io.github.hypercopy.data.RuleRepository
import io.github.hypercopy.data.directIntent
import io.github.hypercopy.data.findRule
import io.github.hypercopy.data.matchRule

object ClipboardTextHandler {
    private const val TAG = "HyperCopy"
    private const val DUPLICATE_WINDOW_MILLIS = 1_500L

    private var lastText: String = ""
    private var lastHandledAt: Long = 0L

    fun handle(context: Context, text: String, source: String) {
        val input = text.trim()
        if (input.isEmpty() || input.length > Config.CLIPBOARD_TEXT_MAX_LENGTH) return

        val now = System.currentTimeMillis()
        if (input == lastText && now - lastHandledAt < DUPLICATE_WINDOW_MILLIS) return
        lastText = input
        lastHandledAt = now

        val appContext = context.applicationContext
        val rules = RuleRepository(appContext).readRules()
        val match = matchRule(input, rules)
        if (match != null) {
            launchRootOrFallback(appContext, match.intent)
            return
        }

        val rule = findRule(input, rules) ?: return
        when (rule.actionMode) {
            RuleActionMode.DirectOpen -> launchRootOrFallback(appContext, rule.directIntent(input))
            RuleActionMode.WebViewResolveAndOpen -> startWebViewResolve(appContext, rule, input)
            RuleActionMode.ParseAndOpen -> return
        }
    }

    private fun startWebViewResolve(context: Context, rule: RuleConfig, input: String) {
        HeadlessWebViewResolver.resolveAndLaunch(context, normalizeUrl(input), rule.target.packageName)
    }

    private fun launchRootOrFallback(context: Context, intent: Intent) {
        val resolvedIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).withResolvedActivity(context.packageManager)
        if (RootActivityLauncher.launch(resolvedIntent)) return
        runCatching {
            context.startActivity(resolvedIntent)
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to start activity", throwable)
            Toast.makeText(context, R.string.toast_open_target_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun normalizeUrl(text: String): String {
        val value = text.trim()
        val uri = runCatching { android.net.Uri.parse(value) }.getOrNull()
        return if (uri?.scheme.isNullOrBlank()) "https://$value" else value
    }
}
