package io.github.hypercopy.ui.rules

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import io.github.hypercopy.R
import io.github.hypercopy.data.RuleCategory
import io.github.hypercopy.data.RuleConfig
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun RuleCategoryTabs(
    selectedCategory: RulePageCategory,
    onSelected: (RulePageCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRowWithContour(
        tabs = ruleCategoryTabTitles.map { stringResource(it) },
        selectedTabIndex = selectedCategory.tabIndex(),
        onTabSelected = { onSelected(rulePageCategoryFromTab(it)) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun TestRuleCard(
    category: RulePageCategory,
    value: String,
    resultText: String,
    onValueChange: (String) -> Unit,
    onExecute: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = stringResource(R.string.rule_test_title, stringResource(category.titleRes())), style = MiuixTheme.textStyles.title3)
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(category.testHintRes()),
                singleLine = false,
                maxLines = 3,
            )
            Text(
                text = resultText,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            TextButton(
                text = stringResource(R.string.action_run_test),
                onClick = onExecute,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
internal fun EmptyRulesCard(category: RulePageCategory) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.rule_empty_title, stringResource(category.titleRes())), style = MiuixTheme.textStyles.title3)
            Text(
                text = stringResource(category.emptyDescriptionRes()),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
internal fun RuleSelectionBar(
    selectedCount: Int,
    allSelected: Boolean,
    onCloseClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(
                onClick = onCloseClick,
                minWidth = 36.dp,
                minHeight = 36.dp,
                cornerRadius = 18.dp,
                backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.08f),
            ) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.action_cancel_selection),
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = stringResource(R.string.rule_selected_count, selectedCount),
                style = MiuixTheme.textStyles.headline1,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onSelectAllClick,
                minWidth = 36.dp,
                minHeight = 36.dp,
                cornerRadius = 18.dp,
                backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.08f),
            ) {
                Icon(
                    imageVector = MiuixIcons.SelectAll,
                    contentDescription = stringResource(if (allSelected) R.string.action_select_none else R.string.action_select_all),
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(
                onClick = onDeleteClick,
                minWidth = 36.dp,
                minHeight = 36.dp,
                cornerRadius = 18.dp,
                backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.08f),
            ) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RuleCard(
    rule: RuleConfig,
    selected: Boolean,
    selectionMode: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelectionToggle: () -> Unit,
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (selectionMode) onSelectionToggle() else onEditClick() },
                    onLongClick = onLongClick,
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PackageIcon(
                packageName = rule.target.packageName,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = rule.name, style = MiuixTheme.textStyles.headline1)
                Text(
                    text = stringResource(ruleActionLabelRes(rule)),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
            if (selectionMode) {
                Checkbox(
                    state = if (selected) ToggleableState.On else ToggleableState.Off,
                    onClick = onSelectionToggle,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            if (!selectionMode) {
                Switch(checked = rule.enabled, onCheckedChange = { onEnabledChange(!rule.enabled) })
                IconButton(
                    onClick = onEditClick,
                    minWidth = 32.dp,
                    minHeight = 32.dp,
                    cornerRadius = 16.dp,
                    backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 10.dp),
                ) {
                    Icon(
                        imageVector = MiuixIcons.ChevronForward,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun AddRuleMenu(
    category: RulePageCategory,
    modifier: Modifier = Modifier,
    onBrowserClick: () -> Unit,
    onLinkRuleClick: () -> Unit,
    onExpressRuleClick: () -> Unit,
) {
    var showPopup by remember { mutableStateOf(false) }
    val items = listOf(stringResource(R.string.rule_menu_browser), stringResource(R.string.action_add_rule))

    Box(modifier = modifier) {
        FloatingActionButton(onClick = { if (category == RulePageCategory.Link) showPopup = true else onExpressRuleClick() }) {
            Icon(
                imageVector = MiuixIcons.Add,
                contentDescription = stringResource(R.string.action_add_rule),
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        OverlayListPopup(
            show = showPopup && category == RulePageCategory.Link,
            alignment = PopupPositionProvider.Align.End,
            onDismissRequest = { showPopup = false },
        ) {
            ListPopupColumn {
                items.forEachIndexed { index, text ->
                    DropdownImpl(
                        text = text,
                        optionSize = items.size,
                        isSelected = false,
                        index = index,
                        onSelectedIndexChange = {
                            showPopup = false
                            if (index == 0) onBrowserClick() else onLinkRuleClick()
                        },
                    )
                }
            }
        }
    }
}
