package io.github.hypercopy.clipboard

import android.content.Context
import android.util.Log
import io.github.hypercopy.Config
import io.github.hypercopy.data.RuleActionMode
import io.github.hypercopy.data.RuleConfig
import io.github.hypercopy.data.RuleRepository
import io.github.hypercopy.data.SettingsRepository
import io.github.hypercopy.data.directIntent
import io.github.hypercopy.data.findRule
import io.github.hypercopy.data.matchRule
import io.github.hypercopy.data.parseIntent
import io.github.hypercopy.data.resolveInputUrl
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
        val ignoreJumpApp = SettingsRepository(appContext).readIgnoreJumpApp()
        val rules = RuleRepository(appContext).readRules()
        val match = matchRule(input, rules)
        if (match != null) {
            if (shouldIgnoreJump(source, match.rule.target.packageName, ignoreJumpApp)) return
            submitJump(
                appContext,
                PendingJump.IntentJump(
                    title = match.rule.name,
                    intent = match.intent,
                    packageName = match.rule.target.packageName,
                ),
                match.rule.clearClipboardAfterJump,
            )
            return
        }

        val rule = findRule(input, rules) ?: return
        when (rule.actionMode) {
            RuleActionMode.DirectOpen -> {
                if (shouldIgnoreJump(source, rule.target.packageName, ignoreJumpApp)) return
                submitJump(
                    appContext,
                    PendingJump.IntentJump(
                        title = rule.name,
                        intent = rule.directIntent(input, appContext.packageManager),
                        packageName = rule.target.packageName,
                    ),
                    rule.clearClipboardAfterJump,
                )
            }
            RuleActionMode.WebViewResolveAndOpen -> {
                if (shouldIgnoreJump(source, rule.target.packageName, ignoreJumpApp)) return
                startWebViewResolve(appContext, rule, input)
            }
            RuleActionMode.ParseAndOpen -> return
        }
    }

    private fun startWebViewResolve(context: Context, rule: RuleConfig, input: String) {
        val resolveUrl = rule.resolveInputUrl(input)
        if (rule.parseAfterRedirect) {
            thread(name = "HyperCopyRedirectResolve") {
                val redirectedUrl = OneRedirectResolver.resolve(resolveUrl)
                Log.d(TAG, "redirect parse url: $redirectedUrl")
                val intent = rule.parseIntent(
                    redirectedUrl,
                    requireMatch = false,
                    extraParameters = mapOf("input" to input, "redirectUrl" to redirectedUrl),
                ) ?: run {
                    Log.d(TAG, "redirect parse no parameters: $redirectedUrl")
                    return@thread
                }
                submitJump(
                    context,
                    PendingJump.IntentJump(
                        title = rule.name,
                        intent = intent,
                        packageName = rule.target.packageName,
                    ),
                    rule.clearClipboardAfterJump,
                )
            }
            return
        }
        submitJump(
            context,
            PendingJump.WebViewJump(
                title = rule.name,
                url = resolveUrl,
                packageName = rule.target.packageName,
            ),
            rule.clearClipboardAfterJump,
        )
    }

    private fun submitJump(context: Context, jump: PendingJump, clearClipboardAfterJump: Boolean) {
        PendingJumpCoordinator.submit(context, jump, clearClipboardAfterJump)
    }

    private fun shouldIgnoreJump(source: String, targetPackageName: String, ignoreJumpApp: Boolean): Boolean {
        return ignoreJumpApp && source.isNotBlank() && source == targetPackageName
    }

}
