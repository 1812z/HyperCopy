package io.github.hypercopy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hypercopy.Config
import io.github.hypercopy.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppListPage(
    workMode: String,
    ignoreJumpApp: Boolean,
    onWorkModeChange: (String) -> Unit,
    onIgnoreJumpAppChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    bottomContentPadding: Dp = 16.dp,
) {
    var searchQuery by remember { mutableStateOf("") }
    var showMenuPopup by remember { mutableStateOf(false) }

    val workModeOptions = listOf(
        AppListWorkModeOption(stringResource(R.string.app_list_whitelist), Config.APP_LIST_WORK_MODE_WHITELIST),
        AppListWorkModeOption(stringResource(R.string.app_list_blacklist), Config.APP_LIST_WORK_MODE_BLACKLIST),
    )
    val menuEntries = listOf(
        DropdownEntry(
            items = listOf(
                DropdownItem(
                    text = stringResource(R.string.app_list_work_mode),
                    children = workModeOptions.map { option ->
                        DropdownItem(
                            text = option.label,
                            selected = workMode == option.value,
                            onClick = { onWorkModeChange(option.value) },
                        )
                    },
                ),
                DropdownItem(
                    text = stringResource(R.string.app_list_ignore_jump),
                    selected = ignoreJumpApp,
                    onClick = { onIgnoreJumpAppChange(!ignoreJumpApp) },
                ),
            ),
        ),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Card(modifier = Modifier.size(42.dp), onClick = onBack) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = stringResource(R.string.action_back))
                    }
                }
                Text(
                    text = stringResource(R.string.app_list),
                    style = MiuixTheme.textStyles.title1,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.app_list_search_hint),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = MiuixIcons.Search,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    },
                )
                Box {
                    IconButton(
                        onClick = { showMenuPopup = true },
                        minWidth = 42.dp,
                        minHeight = 42.dp,
                        cornerRadius = 21.dp,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.ListView,
                            contentDescription = stringResource(R.string.app_list_menu_options),
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    OverlayCascadingListPopup(
                        show = showMenuPopup,
                        entries = menuEntries,
                        onDismissRequest = { showMenuPopup = false },
                    )
                }
            }
        }

    }
}

private data class AppListWorkModeOption(val label: String, val value: String)
