package io.github.hypercopy.data

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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

    fun reorderRules(categories: Set<RuleCategory>, orderedRuleIds: List<String>) {
        if (categories.isEmpty() || orderedRuleIds.isEmpty()) return
        val currentRules = readRules()
        val categoryRules = currentRules.filter { it.category in categories }
        val orderedIds = orderedRuleIds.toSet()
        val orderedRules = orderedRuleIds.mapNotNull { ruleId -> categoryRules.firstOrNull { it.id == ruleId } } +
            categoryRules.filterNot { it.id in orderedIds }
        val categoryIterator = orderedRules.iterator()
        val rules = currentRules.map { rule ->
            if (rule.category in categories && categoryIterator.hasNext()) categoryIterator.next() else rule
        }
        persistRules(rules)
    }

    fun persistRules(rules: List<RuleConfig>) {
        rulesFile().writeText(rulesToJson(rules))
        ruleChanges.tryEmit(Unit)
    }

    private fun rulesFile() = context.filesDir.resolve(RULES_FILE_NAME)

    companion object {
        private val ruleChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val changes = ruleChanges.asSharedFlow()

        const val RULES_FILE_NAME = "rules.json"
    }
}
