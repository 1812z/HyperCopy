package io.github.hypercopy.ui.rules

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hypercopy.R
import io.github.hypercopy.data.CloudRule
import io.github.hypercopy.data.CloudRuleException
import io.github.hypercopy.data.CloudRulesRepository
import io.github.hypercopy.data.RuleRepository
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CloudRulesPage(bottomContentPadding: Dp = 16.dp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudRepository = remember { CloudRulesRepository() }
    val localRepository = remember { RuleRepository(context.applicationContext) }

    var selectedCategory by remember { mutableStateOf(RulePageCategory.Link) }
    var searchQuery by remember { mutableStateOf("") }
    var cloudRules by remember { mutableStateOf<List<CloudRule>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val downloadingIds = remember { mutableStateMapOf<String, Boolean>() }

    fun refreshDownloadedIds() {
        downloadedIds = localRepository.readRules().map { it.id }.toSet()
    }

    fun loadRules(category: RulePageCategory) {
        scope.launch {
            loading = true
            error = null
            runCatching { cloudRepository.listRules(category.folderName()) }
                .onSuccess { cloudRules = it }
                .onFailure { error = (it as? CloudRuleException)?.message ?: context.getString(R.string.cloud_error_load) }
            loading = false
            refreshDownloadedIds()
        }
    }

    LaunchedEffect(selectedCategory) { loadRules(selectedCategory) }

    val filteredRules by remember(cloudRules, searchQuery) {
        derivedStateOf {
            val query = searchQuery.trim()
            if (query.isEmpty()) {
                cloudRules
            } else {
                cloudRules.filter { rule ->
                    rule.name.contains(query, ignoreCase = true) ||
                        rule.packageName.contains(query, ignoreCase = true)
                }
            }
        }
    }

    fun handleDownload(rule: CloudRule) {
        if (downloadingIds[rule.fileName] == true) return
        downloadingIds[rule.fileName] = true
        scope.launch {
            runCatching { cloudRepository.downloadRule(rule) }
                .onSuccess { config ->
                    localRepository.saveRule(config)
                    refreshDownloadedIds()
                    Toast.makeText(context, context.getString(R.string.cloud_toast_added, config.name), Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(
                        context,
                        context.getString(R.string.cloud_toast_download_failed, (it as? CloudRuleException)?.message ?: it.message),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            downloadingIds[rule.fileName] = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CloudRulesHeader(
            title = stringResource(R.string.tab_cloud_rules),
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onRefresh = { loadRules(selectedCategory) },
        )
        RuleCategoryTabs(
            selectedCategory = selectedCategory,
            modifier = Modifier.padding(horizontal = 12.dp),
            onSelected = {
                selectedCategory = it
                searchQuery = ""
            },
        )
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading && cloudRules.isEmpty() -> CloudRulesLoading()
                error != null && cloudRules.isEmpty() -> CloudRulesError(
                    message = error!!,
                    onRetry = { loadRules(selectedCategory) },
                    bottomContentPadding = bottomContentPadding,
                )

                filteredRules.isEmpty() -> CloudRulesEmpty(
                    isSearching = searchQuery.isNotBlank(),
                    bottomContentPadding = bottomContentPadding,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 4.dp,
                        end = 12.dp,
                        bottom = bottomContentPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        SmallTitle(
                            text = stringResource(
                                R.string.cloud_rules_count,
                                filteredRules.size,
                                stringResource(selectedCategory.titleRes()),
                            ),
                        )
                    }
                    items(filteredRules, key = { it.fileName }) { rule ->
                        CloudRuleCard(
                            rule = rule,
                            downloaded = rule.stableId() in downloadedIds,
                            downloading = downloadingIds[rule.fileName] == true,
                            onDownload = { handleDownload(rule) },
                        )
                    }
                }
            }
        }
    }
}

private fun CloudRule.stableId(): String = "cloud_${folder}_${fileNameWithoutExt()}"

@Composable
private fun CloudRulesHeader(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title1,
                modifier = Modifier.weight(1f).padding(top = 8.dp),
            )
            IconButton(
                onClick = onRefresh,
                minWidth = 36.dp,
                minHeight = 36.dp,
                cornerRadius = 18.dp,
                backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.08f),
            ) {
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = stringResource(R.string.action_refresh),
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.cloud_search_hint),
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
    }
}

@Composable
private fun CloudRulesLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.cloud_loading),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun CloudRulesError(message: String, onRetry: () -> Unit, bottomContentPadding: Dp) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = stringResource(R.string.cloud_load_failed), style = MiuixTheme.textStyles.title3)
                    Text(
                        text = message,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    TextButton(
                        text = stringResource(R.string.action_retry),
                        onClick = onRetry,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudRulesEmpty(isSearching: Boolean, bottomContentPadding: Dp) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(if (isSearching) R.string.cloud_no_match else R.string.cloud_empty),
                        style = MiuixTheme.textStyles.title3,
                    )
                    Text(
                        text = if (isSearching) {
                            stringResource(R.string.cloud_no_match_hint)
                        } else {
                            stringResource(R.string.cloud_empty_hint)
                        },
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudRuleCard(
    rule: CloudRule,
    downloaded: Boolean,
    downloading: Boolean,
    onDownload: () -> Unit,
) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PackageIcon(
                packageName = rule.packageName,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = rule.name, style = MiuixTheme.textStyles.headline1)
                Text(
                    text = rule.packageName.ifBlank { stringResource(R.string.cloud_generic_rule) },
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            if (downloading) {
                CircularProgressIndicator(
                    size = 20.dp,
                    strokeWidth = 2.dp,
                )
            } else {
                IconButton(
                    onClick = onDownload,
                    minWidth = 36.dp,
                    minHeight = 36.dp,
                    cornerRadius = 18.dp,
                    backgroundColor = if (downloaded) {
                        Color(0xFF36D167).copy(alpha = 0.12f)
                    } else {
                        MiuixTheme.colorScheme.primary.copy(alpha = 0.08f)
                    },
                ) {
                    Icon(
                        imageVector = if (downloaded) MiuixIcons.Basic.Check else MiuixIcons.Download,
                        contentDescription = stringResource(if (downloaded) R.string.action_update_rule else R.string.action_download_rule),
                        tint = if (downloaded) Color(0xFF36D167) else MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
