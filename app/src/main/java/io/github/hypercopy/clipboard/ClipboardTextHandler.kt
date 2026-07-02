package io.github.hypercopy.clipboard

import android.content.Context
import io.github.hypercopy.Config
import io.github.hypercopy.HyperLog
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
        val settingsRepository = SettingsRepository(appContext)
        val appListWorkMode = settingsRepository.readAppListWorkMode()
        val appListPackages = settingsRepository.readAppListPackages()
        if (shouldSkipByAppList(source, appListWorkMode, appListPackages)) return

        val rules = RuleRepository(appContext).readRules()
        val ignoreJumpApp = settingsRepository.readIgnoreJumpApp()
        if (shouldIgnoreBeforeMatch(source, rules, ignoreJumpApp)) return

        if (settingsRepository.readSystemLinkHandling()) {
            val systemJump = SystemLinkHandler.createJump(appContext, input)
            if (systemJump != null && !shouldIgnoreJump(source, systemJump.packageName, ignoreJumpApp)) {
                submitJump(appContext, systemJump, clearClipboardAfterJump = false)
                return
            }
        }

        val match = matchRule(input, rules)
        if (match != null) {
            val targetPackageName = jumpPackageName(appContext, match.rule.target.packageName, match.intent)
            if (shouldIgnoreJump(source, targetPackageName, ignoreJumpApp)) {
                HyperLog.d(TAG, "ignore jump in target app before notification: source=$source target=$targetPackageName")
                return
            }
            submitJump(
                appContext,
                PendingJump.IntentJump(
                    title = match.rule.name,
                    intent = match.intent,
                    packageName = targetPackageName,
                ),
                match.rule.clearClipboardAfterJump,
            )
            return
        }

        val rule = findRule(input, rules) ?: return
        when (rule.actionMode) {
            RuleActionMode.DirectOpen -> {
                val intent = rule.directIntent(input, appContext.packageManager)
                val targetPackageName = jumpPackageName(appContext, rule.target.packageName, intent)
                if (shouldIgnoreJump(source, targetPackageName, ignoreJumpApp)) {
                    HyperLog.d(TAG, "ignore jump in target app before notification: source=$source target=$targetPackageName")
                    return
                }
                submitJump(
                    appContext,
                    PendingJump.IntentJump(
                        title = rule.name,
                        intent = intent,
                        packageName = targetPackageName,
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
                HyperLog.d(TAG, "redirect parse url: $redirectedUrl")
                val intent = rule.parseIntent(
                    redirectedUrl,
                    requireMatch = false,
                    extraParameters = mapOf("input" to input, "redirectUrl" to redirectedUrl),
                ) ?: run {
                    HyperLog.d(TAG, "redirect parse no parameters: $redirectedUrl")
                    return@thread
                }
                val targetPackageName = jumpPackageName(context, rule.target.packageName, intent)
                submitJump(
                    context,
                    PendingJump.IntentJump(
                        title = rule.name,
                        intent = intent,
                        packageName = targetPackageName,
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
        HyperLog.d(TAG, "submit jump notification: target=${jump.packageName}")
        PendingJumpCoordinator.submit(context, jump, clearClipboardAfterJump)
    }

    private fun shouldIgnoreJump(source: String, targetPackageName: String, ignoreJumpApp: Boolean): Boolean {
        return ignoreJumpApp && source.isNotBlank() && source == targetPackageName
    }

    private fun jumpPackageName(context: Context, configPackageName: String, intent: android.content.Intent): String {
        if (configPackageName.isNotBlank()) return configPackageName
        return intent.`package` ?: intent.component?.packageName ?: intent.resolveActivity(context.packageManager)?.packageName.orEmpty()
    }

    private fun shouldIgnoreBeforeMatch(source: String, rules: List<RuleConfig>, ignoreJumpApp: Boolean): Boolean {
        if (!ignoreJumpApp || source.isBlank()) return false
        return rules.any { it.target.packageName == source }
    }

    private fun shouldSkipByAppList(source: String, workMode: String, packages: Set<String>): Boolean {
        if (source.isBlank()) return false
        return when (workMode) {
            Config.APP_LIST_WORK_MODE_BLACKLIST -> source in packages
            Config.APP_LIST_WORK_MODE_WHITELIST -> source !in packages
            else -> false
        }
    }

}
