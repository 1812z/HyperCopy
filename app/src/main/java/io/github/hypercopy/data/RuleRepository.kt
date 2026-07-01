package io.github.hypercopy.data

import android.content.Context
import io.github.hypercopy.R

class RuleRepository(private val context: Context) {
    fun readRules(): List<RuleConfig> {
        val file = rulesFile()
        val savedRules = if (!file.exists()) emptyList() else runCatching { rulesFromJson(file.readText()) }.getOrDefault(emptyList())
        val rules = savedRules.withDefaultCategoryRules(context)
        if (rules.size != savedRules.size) persistRules(rules)
        return rules
    }

    fun saveRule(rule: RuleConfig) {
        val currentRules = readRules()
        val rules = if (currentRules.any { it.id == rule.id }) {
            currentRules.map { if (it.id == rule.id) rule else it }
        } else {
            currentRules + rule
        }
        persistRules(rules)
    }

    fun setRuleEnabled(ruleId: String, enabled: Boolean) {
        val rules = readRules().map { rule ->
            if (rule.id == ruleId) rule.copy(enabled = enabled) else rule
        }
        persistRules(rules)
    }

    fun deleteRules(ruleIds: Set<String>) {
        if (ruleIds.isEmpty()) return
        persistRules(readRules().filterNot { it.id in ruleIds })
    }

    fun persistRules(rules: List<RuleConfig>) {
        rulesFile().writeText(rulesToJson(rules))
    }

    private fun rulesFile() = context.filesDir.resolve(RULES_FILE_NAME)

    private companion object {
        const val RULES_FILE_NAME = "rules.json"
    }
}

private fun List<RuleConfig>.withDefaultCategoryRules(context: Context): List<RuleConfig> {
    val rules = toMutableList()
    if (rules.none { it.id == DEFAULT_ADDRESS_RULE_ID }) rules += defaultAddressRule(context)
    if (rules.none { it.id == DEFAULT_EXPRESS_RULE_ID }) rules += defaultExpressRule(context)
    return rules
}

private fun defaultAddressRule(context: Context): RuleConfig = RuleConfig(
    id = DEFAULT_ADDRESS_RULE_ID,
    name = context.getString(R.string.default_address_rule_name),
    category = RuleCategory.Address,
    actionMode = RuleActionMode.DirectOpen,
    matchRegex = "(?=.*(地址|省|市|区)).{10,}",
    parameterRegex = "(.+)",
    target = RuleTarget(
        type = RuleTargetType.Url,
        template = "${'$'}{input}",
    ),
)

private fun defaultExpressRule(context: Context): RuleConfig = RuleConfig(
    id = DEFAULT_EXPRESS_RULE_ID,
    name = context.getString(R.string.default_express_rule_name),
    category = RuleCategory.Express,
    actionMode = RuleActionMode.DirectOpen,
    matchRegex = "\\b(?=[A-Za-z0-9]{8,}\\b)[A-Za-z]{0,3}\\d+\\b",
    parameterRegex = "(.+)",
    target = RuleTarget(
        type = RuleTargetType.Url,
        template = "${'$'}{input}",
    ),
)

private const val DEFAULT_ADDRESS_RULE_ID = "default-address-rule"
private const val DEFAULT_EXPRESS_RULE_ID = "default-express-rule"
