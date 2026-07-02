package io.github.hypercopy.data

import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class RuleConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: RuleCategory = RuleCategory.Link,
    val enabled: Boolean = true,
    val actionMode: RuleActionMode = RuleActionMode.ParseAndOpen,
    val matchRegex: String,
    val parameterRegex: String,
    val triggerRegexes: List<String> = emptyList(),
    val extractionRegexes: List<String> = emptyList(),
    val parseAfterRedirect: Boolean = false,
    val target: RuleTarget,
    val clearClipboardAfterJump: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class RuleCategory(val value: String) {
    Link("link"),
    Text("text"),
    Address("address"),
    Express("express"),
}

enum class RuleActionMode(val value: String) {
    ParseAndOpen("parse_and_open"),
    DirectOpen("direct_open"),
    WebViewResolveAndOpen("webview_resolve_and_open"),
}

data class RuleTarget(
    val type: RuleTargetType,
    val template: String,
    val packageName: String = "",
    val action: String = Intent.ACTION_VIEW,
)

enum class RuleTargetType(val value: String) {
    Url("url"),
    Intent("intent"),
}

fun RuleConfig.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("category", category.value)
    .put("enabled", enabled)
    .put("actionMode", actionMode.value)
    .put("matchRegex", matchRegex)
    .put("parameterRegex", parameterRegex)
    .put("triggerRegexes", triggerRegexes.toJsonArray())
    .put("extractionRegexes", extractionRegexes.toJsonArray())
    .put("parseAfterRedirect", parseAfterRedirect)
    .also { json -> if (clearClipboardAfterJump) json.put("clearClipboardAfterJump", true) }
    .put("target", target.toJson())
    .put("createdAt", createdAt)

private fun List<String>.toJsonArray(): JSONArray = JSONArray().also { array ->
    filter { it.isNotBlank() }.forEach { array.put(it) }
}

fun RuleTarget.toJson(): JSONObject = JSONObject()
    .put("type", type.value)
    .put("template", template)
    .put("packageName", packageName)
    .put("action", action)

fun ruleConfigFromJson(json: JSONObject): RuleConfig = RuleConfig(
    id = json.optString("id", UUID.randomUUID().toString()),
    name = json.optString("name", ""),
    category = ruleCategoryFromValue(json.optString("category")),
    enabled = json.optBoolean("enabled", true),
    actionMode = ruleActionModeFromValue(json.optString("actionMode")),
    matchRegex = json.optString("matchRegex"),
    parameterRegex = json.optString("parameterRegex"),
    triggerRegexes = json.optStringArray("triggerRegexes"),
    extractionRegexes = json.optStringArray("extractionRegexes"),
    parseAfterRedirect = json.optBoolean("parseAfterRedirect", false),
    target = ruleTargetFromJson(json.optJSONObject("target") ?: JSONObject()),
    clearClipboardAfterJump = json.optBoolean("clearClipboardAfterJump", false),
    createdAt = json.optLong("createdAt", System.currentTimeMillis()),
)

private fun JSONObject.optStringArray(name: String): List<String> {
    val array = optJSONArray(name) ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val value = array.optString(index)
            if (value.isNotBlank()) add(value)
        }
    }
}

fun ruleCategoryFromValue(value: String): RuleCategory = when (value) {
    RuleCategory.Text.value -> RuleCategory.Text
    RuleCategory.Address.value -> RuleCategory.Address
    RuleCategory.Express.value -> RuleCategory.Express
    else -> RuleCategory.Link
}

fun ruleActionModeFromValue(value: String): RuleActionMode = when (value) {
    RuleActionMode.DirectOpen.value -> RuleActionMode.DirectOpen
    RuleActionMode.WebViewResolveAndOpen.value -> RuleActionMode.WebViewResolveAndOpen
    else -> RuleActionMode.ParseAndOpen
}

fun ruleTargetFromJson(json: JSONObject): RuleTarget {
    val type = when (json.optString("type")) {
        RuleTargetType.Intent.value -> RuleTargetType.Intent
        else -> RuleTargetType.Url
    }
    return RuleTarget(
        type = type,
        template = json.optString("template"),
        packageName = json.optString("packageName"),
        action = json.optString("action", Intent.ACTION_VIEW),
    )
}

fun rulesToJson(rules: List<RuleConfig>): String {
    val root = JSONObject()
    val items = JSONArray()
    rules.forEach { items.put(it.toJson()) }
    return root.put("version", 1).put("rules", items).toString(2)
}

fun rulesFromJson(text: String): List<RuleConfig> {
    if (text.isBlank()) return emptyList()
    val trimmed = text.trim()
    if (trimmed.startsWith("[")) return rulesFromJsonArray(JSONArray(trimmed))

    val root = JSONObject(trimmed)
    val items = root.optJSONArray("rules") ?: return listOf(ruleConfigFromJson(root))
    return rulesFromJsonArray(items)
}

private fun rulesFromJsonArray(items: JSONArray): List<RuleConfig> {
    return buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            add(ruleConfigFromJson(item))
        }
    }
}
