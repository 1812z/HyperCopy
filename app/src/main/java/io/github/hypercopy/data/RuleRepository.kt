package io.github.hypercopy.data

import android.content.Context

class RuleRepository(private val context: Context) {
    fun readRules(): List<RuleConfig> {
        val file = rulesFile()
        return if (!file.exists()) emptyList() else runCatching { rulesFromJson(file.readText()) }.getOrDefault(emptyList())
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
