package io.github.hypercopy.clipboard

import android.content.Intent

object IntentAmStartCommand {
    fun build(intent: Intent): String {
        val args = mutableListOf("am", "start", "--user", "0")
        intent.action?.takeIf { it.isNotBlank() }?.let { args += listOf("-a", it) }
        intent.dataString?.takeIf { it.isNotBlank() }?.let { args += listOf("-d", it) }
        intent.categories?.forEach { category -> args += listOf("-c", category) }
        intent.component?.flattenToString()?.takeIf { it.isNotBlank() }?.let { args += listOf("-n", it) }
        if (intent.component == null) {
            intent.`package`?.takeIf { it.isNotBlank() }?.let { args += listOf("-p", it) }
        }
        intent.extras?.keySet()?.forEach { key ->
            val value = intent.extras?.get(key)
            if (value is String) args += listOf("--es", key, value)
        }
        return args.joinToString(" ") { it.shellQuote() }
    }

    fun String.shellQuote(): String = "'" + replace("'", "'\\''") + "'"
}
