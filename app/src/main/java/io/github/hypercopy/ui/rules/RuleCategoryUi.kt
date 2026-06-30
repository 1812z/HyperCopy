package io.github.hypercopy.ui.rules

import io.github.hypercopy.data.RuleActionMode
import io.github.hypercopy.data.RuleCategory

internal enum class RulePageCategory {
    Link,
    Text,
}

internal val ruleCategoryTabs = listOf("链接", "口令/文本")

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

internal fun RulePageCategory.title(): String = when (this) {
    RulePageCategory.Link -> "链接"
    RulePageCategory.Text -> "口令/文本"
}

internal fun RulePageCategory.testHint(): String = when (this) {
    RulePageCategory.Link -> "输入复制后的 URL 或文本"
    RulePageCategory.Text -> "输入复制后的口令或文本"
}

internal fun RulePageCategory.emptyDescription(): String = when (this) {
    RulePageCategory.Link -> "点击右下角 + 打开内置浏览器，或手动添加链接规则。"
    RulePageCategory.Text -> "口令和文本规则支持直接打开 App 或打开 URL。默认规则可点进编辑修改。"
}

internal fun RuleCategory.title(): String = when (this) {
    RuleCategory.Link -> "链接"
    RuleCategory.Address -> "地址"
    RuleCategory.Express -> "快递"
}

internal fun RuleActionMode.label(): String = when (this) {
    RuleActionMode.ParseAndOpen -> "解析参数打开"
    RuleActionMode.DirectOpen -> "直接打开 App"
    RuleActionMode.WebViewResolveAndOpen -> "WebView 模拟打开"
}

internal fun ruleActionLabel(rule: io.github.hypercopy.data.RuleConfig): String {
    if (rule.category != RuleCategory.Link) {
        return if (rule.target.packageName.isBlank()) "打开 URL" else "直接打开 App"
    }
    return rule.actionMode.label()
}
