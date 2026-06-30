package io.github.hypercopy.clipboard

import android.content.Context
import android.util.Log
import io.github.hypercopy.Config
import io.github.hypercopy.data.RuleActionMode
import io.github.hypercopy.data.RuleConfig
import io.github.hypercopy.data.RuleRepository
import io.github.hypercopy.data.directIntent
import io.github.hypercopy.data.findRule
import io.github.hypercopy.data.matchRule
import io.github.hypercopy.data.parseIntent
import kotlin.concurrent.thread

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
            submitJump(appContext, PendingJump.IntentJump(match.rule.name, match.intent))
            return
        }

        val rule = findRule(input, rules) ?: return
        when (rule.actionMode) {
            RuleActionMode.DirectOpen -> submitJump(appContext, PendingJump.IntentJump(rule.name, rule.directIntent(input, appContext.packageManager)))
            RuleActionMode.WebViewResolveAndOpen -> startWebViewResolve(appContext, rule, input)
            RuleActionMode.ParseAndOpen -> return
        }
    }

    private fun startWebViewResolve(context: Context, rule: RuleConfig, input: String) {
        if (rule.parseAfterRedirect) {
            thread(name = "HyperCopyRedirectResolve") {
                val redirectedUrl = OneRedirectResolver.resolve(normalizeUrl(input))
                Log.d(TAG, "redirect parse url: $redirectedUrl")
                val intent = rule.parseIntent(
                    redirectedUrl,
                    requireMatch = false,
                    extraParameters = mapOf("input" to input, "redirectUrl" to redirectedUrl),
                ) ?: run {
                    Log.d(TAG, "redirect parse no parameters: $redirectedUrl")
                    return@thread
                }
                submitJump(context, PendingJump.IntentJump(rule.name, intent))
            }
            return
        }
        submitJump(context, PendingJump.WebViewJump(rule.name, normalizeUrl(input), rule.target.packageName))
    }

    private fun submitJump(context: Context, jump: PendingJump) {
        PendingJumpCoordinator.submit(context, jump)
    }

    private fun normalizeUrl(text: String): String {
        val value = text.trim()
        val uri = runCatching { android.net.Uri.parse(value) }.getOrNull()
        return if (uri?.scheme.isNullOrBlank()) "https://$value" else value
    }
}
