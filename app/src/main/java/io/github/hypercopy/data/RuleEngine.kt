package io.github.hypercopy.data

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

data class RuleMatch(val rule: RuleConfig, val parameters: Map<String, String>, val intent: Intent)

fun matchRule(text: String, rules: List<RuleConfig>): RuleMatch? {
    return rules.firstNotNullOfOrNull { rule ->
        if (!rule.enabled) return@firstNotNullOfOrNull null
        if (rule.actionMode != RuleActionMode.ParseAndOpen) return@firstNotNullOfOrNull null
        if (!rule.matchesInput(text)) return@firstNotNullOfOrNull null
        val parameters = rule.extractParameters(text)
        if (rule.extractionPatterns().isNotEmpty() && parameters.none { it.key.startsWith("r") }) return@firstNotNullOfOrNull null
        RuleMatch(rule, parameters, rule.target.toIntent(parameters + ("input" to text)))
    }
}

fun findRule(text: String, rules: List<RuleConfig>): RuleConfig? {
    return rules.firstOrNull { rule ->
        rule.enabled && rule.matchesInput(text)
    }
}

fun RuleConfig.directIntent(text: String, packageManager: PackageManager? = null): Intent {
    if (actionMode == RuleActionMode.DirectOpen && target.packageName.isNotBlank()) {
        return (packageManager?.getLaunchIntentForPackage(target.packageName) ?: Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(target.packageName)
        }).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    val parameters = extractParameters(text) + ("input" to text)
    val template = target.template.ifBlank { "${'$'}{input}" }
    return target.copy(template = template).toIntent(parameters)
}

fun RuleConfig.parseIntent(text: String, requireMatch: Boolean = true, extraParameters: Map<String, String> = emptyMap()): Intent? {
    if (requireMatch && !matchesInput(text)) return null
    val parameters = extractParameters(text)
    if (extractionPatterns().isNotEmpty() && parameters.none { it.key.startsWith("r") }) return null
    return target.toIntent(parameters + extraParameters + ("input" to text))
}

fun RuleConfig.matchesInput(text: String): Boolean {
    val patterns = triggerPatterns()
    if (patterns.isEmpty()) return true
    return patterns.any { pattern -> runCatching { Regex(pattern).containsMatchIn(text) }.getOrDefault(false) }
}

fun RuleConfig.extractParameters(text: String): Map<String, String> {
    val values = mutableMapOf<String, String>()
    var legacyIndex = 1
    extractionPatterns().forEachIndexed { patternIndex, pattern ->
        val match = runCatching { Regex(pattern).find(text) }.getOrNull() ?: return@forEachIndexed
        match.groups.drop(1).forEachIndexed { groupIndex, group ->
            val value = group?.value ?: return@forEachIndexed
            if (groupIndex == 0) values["r${patternIndex + 1}"] = value
            values["r${patternIndex + 1}_${groupIndex + 1}"] = value
            values["p${legacyIndex++}"] = value
        }
    }
    return values
}

fun RuleConfig.triggerPatterns(): List<String> = triggerRegexes.ifEmpty { listOf(matchRegex) }.filter { it.isNotBlank() }

fun RuleConfig.extractionPatterns(): List<String> = extractionRegexes.ifEmpty { listOf(parameterRegex) }.filter { it.isNotBlank() }

fun RuleTarget.toIntent(parameters: Map<String, String>): Intent {
    val resolved = parameters.entries.fold(template) { value, entry ->
        value.replace("${'$'}{${entry.key}}", Uri.encode(entry.value))
    }
    return when (type) {
        RuleTargetType.Intent -> runCatching { Intent.parseUri(resolved, Intent.URI_INTENT_SCHEME) }
            .getOrElse { Intent(action, Uri.parse(resolved)) }
        RuleTargetType.Url -> Intent(action, Uri.parse(resolved))
    }.apply {
        if (packageName.isNotBlank()) setPackage(packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
