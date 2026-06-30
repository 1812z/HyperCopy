package io.github.hypercopy.clipboard

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import io.github.hypercopy.Config
import io.github.hypercopy.data.RuleActionMode
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
        val intent = matchRule(input, rules)?.intent ?: run {
            val rule = findRule(input, rules) ?: return
            when (rule.actionMode) {
                RuleActionMode.DirectOpen -> rule.directIntent(input)
                RuleActionMode.ParseAndOpen -> return
                RuleActionMode.WebViewResolveAndOpen -> return
            }
        }

        runCatching {
            appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to handle clipboard text from $source", throwable)
            Toast.makeText(appContext, "打开目标应用失败", Toast.LENGTH_SHORT).show()
        }
    }
}
