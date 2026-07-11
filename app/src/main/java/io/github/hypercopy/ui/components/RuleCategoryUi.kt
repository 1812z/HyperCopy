package io.github.hypercopy.ui.components

import androidx.annotation.StringRes
import io.github.hypercopy.R
import io.github.hypercopy.data.RuleActionMode
import io.github.hypercopy.data.RuleCategory

internal enum class RulePageCategory {
    System,
    Link,
    Text,
}

@StringRes
internal fun rulePageTabTitle(category: RulePageCategory): Int = when (category) {
    RulePageCategory.System -> R.string.category_system
    RulePageCategory.Link -> R.string.category_link
    RulePageCategory.Text -> R.string.category_text
}

internal val localRuleCategoryTabTitles: List<Int> = listOf(
    R.string.category_system,
    R.string.category_link,
    R.string.category_text,
)

internal val cloudRuleCategoryTabTitles: List<Int> = listOf(
    R.string.category_link,
    R.string.category_text,
)

internal fun RulePageCategory.tabIndex(): Int = when (this) {
    RulePageCategory.System -> 0
    RulePageCategory.Link -> 1
    RulePageCategory.Text -> 2
}

internal fun localRulePageCategoryFromTab(index: Int): RulePageCategory = when (index) {
    1 -> RulePageCategory.Link
    2 -> RulePageCategory.Text
    else -> RulePageCategory.System
}

internal fun cloudRulePageCategoryFromTab(index: Int): RulePageCategory = when (index) {
    1 -> RulePageCategory.Text
    else -> RulePageCategory.Link
}

internal fun RulePageCategory.cloudTabIndex(): Int = when (this) {
    RulePageCategory.Text -> 1
    else -> 0
}

internal fun RulePageCategory.ruleCategories(): Set<RuleCategory> = when (this) {
    RulePageCategory.System -> emptySet()
    RulePageCategory.Link -> setOf(RuleCategory.Link)
    RulePageCategory.Text -> setOf(RuleCategory.Text, RuleCategory.Address, RuleCategory.Express)
}

@StringRes
internal fun RulePageCategory.titleRes(): Int = when (this) {
    RulePageCategory.System -> R.string.category_system
    RulePageCategory.Link -> R.string.category_link
    RulePageCategory.Text -> R.string.category_text
}

internal fun RulePageCategory.folderName(): String = when (this) {
    RulePageCategory.System -> "system"
    RulePageCategory.Link -> "link"
    RulePageCategory.Text -> "text"
}

@StringRes
internal fun RulePageCategory.testHintRes(): Int = when (this) {
    RulePageCategory.System -> R.string.rule_test_link_hint
    RulePageCategory.Link -> R.string.rule_test_link_hint
    RulePageCategory.Text -> R.string.rule_test_text_hint
}

@StringRes
internal fun RulePageCategory.emptyDescriptionRes(): Int = when (this) {
    RulePageCategory.System -> R.string.rule_system_empty_description
    RulePageCategory.Link -> R.string.rule_empty_link_description
    RulePageCategory.Text -> R.string.rule_empty_text_description
}

@StringRes
internal fun RuleCategory.titleRes(): Int = when (this) {
    RuleCategory.Link -> R.string.category_link
    RuleCategory.Text -> R.string.category_text
    RuleCategory.Address -> R.string.category_address
    RuleCategory.Express -> R.string.category_express
}

@StringRes
internal fun RuleActionMode.labelRes(): Int = when (this) {
    RuleActionMode.ParseAndOpen -> R.string.rule_action_parse_and_open
    RuleActionMode.DirectOpen -> R.string.rule_action_direct_open_app
    RuleActionMode.WebViewResolveAndOpen -> R.string.rule_action_webview_open
}

@StringRes
internal fun ruleActionLabelRes(rule: io.github.hypercopy.data.RuleConfig): Int {
    if (rule.category != RuleCategory.Link) {
        return if (rule.target.template.isBlank()) R.string.rule_action_direct_open_app else R.string.rule_action_open_url
    }
    return rule.actionMode.labelRes()
}
