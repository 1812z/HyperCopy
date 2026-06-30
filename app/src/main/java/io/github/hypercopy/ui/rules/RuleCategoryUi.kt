package io.github.hypercopy.ui.rules

import androidx.annotation.StringRes
import io.github.hypercopy.R
import io.github.hypercopy.data.RuleActionMode
import io.github.hypercopy.data.RuleCategory

internal enum class RulePageCategory {
    Link,
    Text,
}

@StringRes
internal fun rulePageTabTitle(category: RulePageCategory): Int = when (category) {
    RulePageCategory.Link -> R.string.category_link
    RulePageCategory.Text -> R.string.page_category_text
}

internal val ruleCategoryTabTitles: List<Int> = listOf(
    R.string.category_link,
    R.string.page_category_text,
)

internal fun RulePageCategory.tabIndex(): Int = when (this) {
    RulePageCategory.Link -> 0
    RulePageCategory.Text -> 1
}

internal fun rulePageCategoryFromTab(index: Int): RulePageCategory = when (index) {
    1 -> RulePageCategory.Text
    else -> RulePageCategory.Link
}

internal fun RulePageCategory.ruleCategories(): Set<RuleCategory> = when (this) {
    RulePageCategory.Link -> setOf(RuleCategory.Link)
    RulePageCategory.Text -> setOf(RuleCategory.Address, RuleCategory.Express)
}

@StringRes
internal fun RulePageCategory.titleRes(): Int = when (this) {
    RulePageCategory.Link -> R.string.category_link
    RulePageCategory.Text -> R.string.page_category_text
}

internal fun RulePageCategory.folderName(): String = when (this) {
    RulePageCategory.Link -> "link"
    RulePageCategory.Text -> "text"
}

@StringRes
internal fun RulePageCategory.testHintRes(): Int = when (this) {
    RulePageCategory.Link -> R.string.rule_test_link_hint
    RulePageCategory.Text -> R.string.rule_test_text_hint
}

@StringRes
internal fun RulePageCategory.emptyDescriptionRes(): Int = when (this) {
    RulePageCategory.Link -> R.string.rule_empty_link_description
    RulePageCategory.Text -> R.string.rule_empty_text_description
}

@StringRes
internal fun RuleCategory.titleRes(): Int = when (this) {
    RuleCategory.Link -> R.string.category_link
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
        return if (rule.target.packageName.isBlank()) R.string.rule_action_open_url else R.string.rule_action_direct_open_app
    }
    return rule.actionMode.labelRes()
}
