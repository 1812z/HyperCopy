package io.github.hypercopy.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hypercopy.Config
import io.github.hypercopy.R
import io.github.hypercopy.ui.rules.PackageIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
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
    selectedPackages: Set<String>,
    onWorkModeChange: (String) -> Unit,
    onIgnoreJumpAppChange: (Boolean) -> Unit,
    onPackageCheckedChange: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    bottomContentPadding: Dp = 16.dp,
) {
    val context = LocalContext.current.applicationContext
    var searchQuery by remember { mutableStateOf("") }
    var showMenuPopup by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var isLoading by remember { mutableStateOf(true) }

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
    val filteredApps by remember(apps, searchQuery) {
        derivedStateOf {
            val query = searchQuery.trim()
            if (query.isEmpty()) {
                apps
            } else {
                apps.filter {
                    it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadInstalledApps(context.packageManager) }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    IconButton(
                        onClick = onBack,
                        minWidth = 42.dp,
                        minHeight = 42.dp,
                    ) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = stringResource(R.string.action_back))
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

            if (!isLoading) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppListItem(
                        app = app,
                        checked = app.packageName in selectedPackages,
                        onCheckedChange = { onPackageCheckedChange(app.packageName, it) },
                    )
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun AppListItem(app: InstalledApp, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppListItemMargin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PackageIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(text = app.label, style = MiuixTheme.textStyles.headline1)
                Text(
                    text = app.packageName,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun loadInstalledApps(packageManager: PackageManager): List<InstalledApp> {
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(launcherIntent, 0)
    }
    return activities
        .filter { it.activityInfo?.packageName?.isNotBlank() == true }
        .distinctBy { it.activityInfo.packageName }
        .map {
            InstalledApp(
                label = it.loadLabel(packageManager).toString(),
                packageName = it.activityInfo.packageName,
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, InstalledApp::label))
}

private data class AppListWorkModeOption(val label: String, val value: String)

private data class InstalledApp(val label: String, val packageName: String)

private val AppListItemMargin = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
