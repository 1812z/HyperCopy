package io.github.hypercopy.data

import android.content.Intent
import android.net.Uri

data class RuleMatch(val rule: RuleConfig, val parameters: Map<String, String>, val intent: Intent)

fun matchRule(text: String, rules: List<RuleConfig>): RuleMatch? {
    return rules.firstNotNullOfOrNull { rule ->
        if (!rule.enabled) return@firstNotNullOfOrNull null
        if (rule.actionMode != RuleActionMode.ParseAndOpen) return@firstNotNullOfOrNull null
        val matchRegex = runCatching { Regex(rule.matchRegex) }.getOrNull() ?: return@firstNotNullOfOrNull null
        val parameterRegex = runCatching { Regex(rule.parameterRegex) }.getOrNull() ?: return@firstNotNullOfOrNull null
        if (!matchRegex.containsMatchIn(text)) return@firstNotNullOfOrNull null
        val match = parameterRegex.find(text) ?: return@firstNotNullOfOrNull null
        val parameters = match.groups.drop(1).mapIndexedNotNull { index, group ->
            group?.value?.let { "p${index + 1}" to it }
        }.toMap()
        RuleMatch(rule, parameters, rule.target.toIntent(parameters + ("input" to text)))
    }
}

fun findRule(text: String, rules: List<RuleConfig>): RuleConfig? {
    return rules.firstOrNull { rule ->
        rule.enabled && runCatching { Regex(rule.matchRegex).containsMatchIn(text) }.getOrDefault(false)
    }
}

fun RuleConfig.directIntent(text: String): Intent {
    val uriText = target.template.ifBlank { text }.replace("${'$'}{input}", text)
    return target.copy(template = uriText).toIntent(emptyMap())
}

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
