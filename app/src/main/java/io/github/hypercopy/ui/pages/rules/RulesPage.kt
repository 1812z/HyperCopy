package io.github.hypercopy.ui.pages.rules

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hypercopy.R
import io.github.hypercopy.HyperLog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.hypercopy.clipboard.OneRedirectResolver
import io.github.hypercopy.data.RuleActionMode
import io.github.hypercopy.data.RuleCategory
import io.github.hypercopy.data.RuleConfig
import io.github.hypercopy.data.RuleRepository
import io.github.hypercopy.data.SettingsRepository
import io.github.hypercopy.data.SystemLinkApp
import io.github.hypercopy.data.SystemLinkRepository
import io.github.hypercopy.data.RuleTarget
import io.github.hypercopy.data.RuleTargetType
import io.github.hypercopy.data.directIntent
import io.github.hypercopy.data.extractFirstInputUrl
import io.github.hypercopy.data.findRule
import io.github.hypercopy.data.matchRule
import io.github.hypercopy.data.parseIntent
import io.github.hypercopy.data.resolveInputUrl
import io.github.hypercopy.data.rulesFromJson
import io.github.hypercopy.data.toIntent
import io.github.hypercopy.ui.activities.RuleBrowserActivity
import io.github.hypercopy.ui.activities.RuleEditorActivity
import io.github.hypercopy.ui.activities.SystemLinkAppDetailActivity
import io.github.hypercopy.ui.components.AddRuleMenu
import io.github.hypercopy.ui.components.EmptyRulesCard
import io.github.hypercopy.ui.components.HiddenWebViewResolver
import io.github.hypercopy.ui.components.HyperSearchBar
import io.github.hypercopy.ui.components.RuleCard
import io.github.hypercopy.ui.components.RuleCategoryTabs
import io.github.hypercopy.ui.components.RuleEditBar
import io.github.hypercopy.ui.components.RulePageCategory
import io.github.hypercopy.ui.components.RuleSelectionBar
import io.github.hypercopy.ui.components.SystemLinkAppListCard
import io.github.hypercopy.ui.components.SystemLinkHandlingCard
import io.github.hypercopy.ui.components.TestRuleCard
import io.github.hypercopy.ui.components.ruleCategories
import io.github.hypercopy.ui.components.titleRes
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.floor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RulesPage(
    modifier: Modifier = Modifier,
    showImportDialog: Boolean = false,
    onDismissImportDialog: () -> Unit = {},
    sortMode: Boolean = false,
    onSortModeChange: (Boolean) -> Unit = {},
    editMode: Boolean = false,
    onEditModeChange: (Boolean) -> Unit = {},
    onRuleActionsAvailableChange: (Boolean) -> Unit = {},
    topContentPadding: Dp = 12.dp,
    bottomContentPadding: Dp = 16.dp,
    systemLinkUserId: Int = 0,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember { RuleRepository(context.applicationContext) }
    val settingsRepository = remember { SettingsRepository(context.applicationContext) }
    val systemLinkRepository = remember { SystemLinkRepository(context.applicationContext) }
    var rules by remember { mutableStateOf(repository.readRules()) }
    var systemLinkHandling by remember { mutableStateOf(settingsRepository.readSystemLinkHandling()) }
    var systemLinkClearClipboardAfterJump by remember { mutableStateOf(settingsRepository.readSystemLinkClearClipboardAfterJump()) }
    var selectedCategory by remember { mutableStateOf(RulePageCategory.System) }
    var testInput by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }
    val ruleResultWaiting = stringResource(R.string.rule_result_waiting)
    var resultText by remember(ruleResultWaiting) { mutableStateOf(ruleResultWaiting) }
    var systemLinkApps by remember { mutableStateOf<List<SystemLinkApp>>(emptyList()) }
    var systemLinkLoading by remember { mutableStateOf(false) }
    var resolvingUrl by remember { mutableStateOf<String?>(null) }
    var resolvingRule by remember { mutableStateOf<RuleConfig?>(null) }
    var selectedRuleIds by remember { mutableStateOf(emptySet<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var sortedCategoryRules by remember { mutableStateOf<List<RuleConfig>?>(null) }
    var draggingRuleId by remember { mutableStateOf<String?>(null) }
    var dragTotalOffsetY by remember { mutableStateOf(0f) }
    var dragMovedSteps by remember { mutableStateOf(0) }
    val itemSpacingPx = remember(density) { with(density) { 12.dp.toPx() } }
    val fallbackRuleItemStepPx = remember(density) { with(density) { 84.dp.toPx() } }
    var ruleItemStepPx by remember { mutableStateOf(0f) }

    DisposableEffect(lifecycleOwner, repository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) rules = repository.readRules()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(repository) {
        RuleRepository.changes.collect {
            rules = repository.readRules()
        }
    }

    val categoryRules = rules.filter { it.category in selectedCategory.ruleCategories() }
    val displayedCategoryRules = if (sortMode) sortedCategoryRules ?: categoryRules else categoryRules
    val filteredCategoryRules = if (sortMode || editMode) {
        displayedCategoryRules
    } else {
        displayedCategoryRules.filter { rule ->
            searchText.isBlank() || rule.name.contains(searchText, ignoreCase = true) ||
                rule.target.packageName.contains(searchText, ignoreCase = true)
        }
    }
    val filteredSystemLinkApps = systemLinkApps.filter { app ->
        searchText.isBlank() || app.label.contains(searchText, ignoreCase = true) ||
            app.packageName.contains(searchText, ignoreCase = true)
    }
    val categoryRuleIds = categoryRules.map { it.id }.toSet()
    val selectionMode = selectedRuleIds.isNotEmpty() || editMode

    BackHandler(enabled = sortMode && selectedCategory != RulePageCategory.System) {
        onSortModeChange(false)
    }

    BackHandler(enabled = editMode && selectedCategory != RulePageCategory.System) {
        onEditModeChange(false)
    }

    BackHandler(enabled = selectedRuleIds.isNotEmpty() && !showDeleteDialog && !editMode && !sortMode) {
        selectedRuleIds = emptySet()
    }

    LaunchedEffect(selectedCategory, categoryRuleIds) {
        selectedRuleIds = selectedRuleIds.intersect(categoryRuleIds)
        if (selectedCategory == RulePageCategory.System) {
            onSortModeChange(false)
            onEditModeChange(false)
        }
        onRuleActionsAvailableChange(selectedCategory != RulePageCategory.System)
    }

    LaunchedEffect(sortMode, selectedCategory, categoryRuleIds) {
        if (sortMode) {
            selectedRuleIds = emptySet()
            sortedCategoryRules = categoryRules
            if (selectedCategory == RulePageCategory.System) onSortModeChange(false)
        } else {
            sortedCategoryRules = null
            draggingRuleId = null
            dragTotalOffsetY = 0f
            dragMovedSteps = 0
        }
    }

    LaunchedEffect(editMode, selectedCategory) {
        if (editMode) {
            selectedRuleIds = emptySet()
            if (selectedCategory == RulePageCategory.System) onEditModeChange(false)
        }
    }

    fun moveSortingRule(ruleId: String, direction: Int): Boolean {
        val currentRules = sortedCategoryRules ?: categoryRules
        val fromIndex = currentRules.indexOfFirst { it.id == ruleId }
        if (fromIndex < 0) return false
        val toIndex = (fromIndex + direction).coerceIn(currentRules.indices)
        if (fromIndex == toIndex) return false
        sortedCategoryRules = currentRules.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        return true
    }

    fun dragRule(ruleId: String, deltaY: Float) {
        val step = ruleItemStepPx.takeIf { it > 0f } ?: fallbackRuleItemStepPx
        dragTotalOffsetY += deltaY
        val targetSteps = if (dragTotalOffsetY >= 0f) {
            floor(dragTotalOffsetY / step).toInt()
        } else {
            ceil(dragTotalOffsetY / step).toInt()
        }
        while (dragMovedSteps < targetSteps && moveSortingRule(ruleId, 1)) {
            dragMovedSteps++
        }
        while (dragMovedSteps > targetSteps && moveSortingRule(ruleId, -1)) {
            dragMovedSteps--
        }
    }

    fun persistSorting() {
        val sortedRules = sortedCategoryRules ?: return
        repository.reorderRules(selectedCategory.ruleCategories(), sortedRules.map { it.id })
        rules = repository.readRules()
        sortedCategoryRules = rules.filter { it.category in selectedCategory.ruleCategories() }
    }

    fun loadSystemLinks() {
        systemLinkLoading = true
        thread(name = "HyperCopySystemLinks") {
            val apps = runCatching { systemLinkRepository.readApps(systemLinkUserId) }
                .getOrElse { throwable ->
                    HyperLog.d("HyperCopy", "load system links failed", throwable)
                    (context as? android.app.Activity)?.runOnUiThread {
                        resultText = context.getString(R.string.rule_system_load_failed, throwable.message.orEmpty())
                    }
                    emptyList()
                }
            (context as? android.app.Activity)?.runOnUiThread {
                systemLinkApps = apps
                systemLinkLoading = false
            }
        }
    }

    LaunchedEffect(systemLinkUserId) {
        if (selectedCategory == RulePageCategory.System) loadSystemLinks()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = topContentPadding, end = 12.dp, bottom = bottomContentPadding + 84.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RuleCategoryTabs(
                    selectedCategory = selectedCategory,
                    includeSystem = true,
                    onSelected = {
                        selectedCategory = it
                        resultText = ruleResultWaiting
                        selectedRuleIds = emptySet()
                        if (it == RulePageCategory.System) loadSystemLinks()
                    },
                )
            }
            if (selectedCategory == RulePageCategory.System) {
                item {
                    SystemLinkHandlingCard(
                        checked = systemLinkHandling,
                        clearClipboardAfterJump = systemLinkClearClipboardAfterJump,
                        onCheckedChange = {
                            systemLinkHandling = it
                            settingsRepository.persistSystemLinkHandling(it)
                        },
                        onClearClipboardAfterJumpChange = {
                            systemLinkClearClipboardAfterJump = it
                            settingsRepository.persistSystemLinkClearClipboardAfterJump(it)
                        },
                    )
                }
            }
            if (!sortMode && !selectionMode) {
                item {
                    TestRuleCard(
                        category = selectedCategory,
                        value = testInput,
                        resultText = resultText,
                        onValueChange = { testInput = it },
                        onExecute = {
                            if (selectedCategory == RulePageCategory.System) {
                                resultText = context.getString(R.string.rule_system_test_running, systemLinkUserId)
                                thread(name = "HyperCopySystemLinkTest") {
                                    val inputUrl = extractFirstInputUrl(testInput)
                                    val success = inputUrl?.let { systemLinkRepository.openLink(systemLinkUserId, it) } == true
                                    (context as? android.app.Activity)?.runOnUiThread {
                                        resultText = context.getString(
                                            if (success) R.string.rule_system_test_started else R.string.rule_result_launch_failed,
                                            if (success) systemLinkUserId.toString() else "no url found",
                                        )
                                    }
                                }
                            } else {
                                resultText = executeRuleTest(
                                    context = context,
                                    input = testInput,
                                    rules = categoryRules,
                                    category = selectedCategory,
                                    onStartWebViewResolve = { url, rule ->
                                        resolvingUrl = url
                                        resolvingRule = rule
                                    },
                                    onStartRedirectParse = { url, rule ->
                                        resultText = context.getString(R.string.rule_result_match_redirect_parse, rule.name)
                                        thread(name = "HyperCopyRedirectTest") {
                                            val redirectedUrl = OneRedirectResolver.resolve(url)
                                            val intent = rule.parseIntent(
                                                redirectedUrl,
                                                requireMatch = false,
                                                extraParameters = mapOf("input" to testInput.trim(), "redirectUrl" to redirectedUrl),
                                            )
                                            (context as? android.app.Activity)?.runOnUiThread {
                                                if (intent == null) {
                                                    resultText = context.getString(R.string.rule_result_redirect_parse_no_param, redirectedUrl)
                                                } else {
                                                    resultText = runCatching { context.startActivity(intent) }
                                                        .fold(
                                                            onSuccess = { context.getString(R.string.rule_result_match_parse_open, rule.name, intent.data) },
                                                            onFailure = { context.getString(R.string.rule_result_launch_failed, it.message) },
                                                        )
                                                }
                                            }
                                        }
                                    },
                                    onStartActivity = { context.startActivity(it) },
                                )
                            }
                        },
                    )
                }
                item {
                    HyperSearchBar(
                        query = searchText,
                        onQueryChange = { searchText = it },
                        label = stringResource(R.string.rule_search_hint),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (selectedCategory == RulePageCategory.System) {
                when {
                    systemLinkLoading -> item { EmptyRulesCard(RulePageCategory.System) }
                    filteredSystemLinkApps.isEmpty() -> item { EmptyRulesCard(RulePageCategory.System) }
                    else -> items(filteredSystemLinkApps, key = { it.packageName }) { app ->
                        SystemLinkAppListCard(
                            app = app,
                            onClick = {
                                context.startActivity(
                                    Intent(context, SystemLinkAppDetailActivity::class.java)
                                        .putExtra(SystemLinkAppDetailActivity.EXTRA_PACKAGE_NAME, app.packageName)
                                        .putExtra(SystemLinkAppDetailActivity.EXTRA_USER_ID, systemLinkUserId)
                                        .putExtra(SystemLinkAppDetailActivity.EXTRA_APP_LABEL, app.label),
                                )
                            },
                            onAppEnabledChange = { enabled ->
                                toggleSystemLinkApp(context, systemLinkRepository, systemLinkUserId, app, enabled) { apps ->
                                    systemLinkApps = apps
                                }
                            },
                        )
                    }
                }
            } else if (filteredCategoryRules.isEmpty()) {
                item { EmptyRulesCard(selectedCategory) }
            } else {
                items(filteredCategoryRules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        selected = rule.id in selectedRuleIds,
                        selectionMode = selectionMode,
                        sortMode = sortMode,
                        dragging = draggingRuleId == rule.id,
                        dragOffsetY = if (draggingRuleId == rule.id) {
                            dragTotalOffsetY - dragMovedSteps * (ruleItemStepPx.takeIf { it > 0f } ?: fallbackRuleItemStepPx)
                        } else {
                            0f
                        },
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                if (coordinates.size.height > 0) ruleItemStepPx = coordinates.size.height + itemSpacingPx
                            }
                            .let { cardModifier -> if (draggingRuleId == rule.id) cardModifier else cardModifier.animateItem() },
                        onEnabledChange = { enabled ->
                            repository.setRuleEnabled(rule.id, enabled)
                            rules = repository.readRules()
                        },
                        onEditClick = {
                            context.startActivity(
                                Intent(context, RuleEditorActivity::class.java)
                                    .putExtra(RuleEditorActivity.EXTRA_RULE_ID, rule.id)
                                    .putExtra(RuleEditorActivity.EXTRA_CATEGORY, rule.category.value),
                            )
                        },
                        onLongClick = {
                            selectedRuleIds = selectedRuleIds + rule.id
                        },
                        onSelectionToggle = {
                            selectedRuleIds = if (rule.id in selectedRuleIds) {
                                selectedRuleIds - rule.id
                            } else {
                                selectedRuleIds + rule.id
                            }
                        },
                        onDragStart = {
                            draggingRuleId = rule.id
                            dragTotalOffsetY = 0f
                            dragMovedSteps = 0
                        },
                        onDrag = { deltaY -> dragRule(rule.id, deltaY) },
                        onDragEnd = {
                            persistSorting()
                            draggingRuleId = null
                            dragTotalOffsetY = 0f
                            dragMovedSteps = 0
                        },
                    )
                }
            }
        }

        if (sortMode && selectedCategory != RulePageCategory.System) {
            RuleEditBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 12.dp, top = topContentPadding, end = 12.dp),
                onCloseClick = { onSortModeChange(false) },
            )
        }

        if (editMode && selectedCategory != RulePageCategory.System) {
            RuleSelectionBar(
                selectedCount = selectedRuleIds.size,
                allSelected = selectedRuleIds.size == categoryRules.size,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 12.dp, top = topContentPadding, end = 12.dp),
                onCloseClick = {
                    selectedRuleIds = emptySet()
                    onEditModeChange(false)
                },
                onSelectAllClick = {
                    selectedRuleIds = if (selectedRuleIds.size == categoryRules.size) {
                        emptySet()
                    } else {
                        categoryRuleIds
                    }
                },
                onDeleteClick = {
                    if (selectedRuleIds.isNotEmpty()) showDeleteDialog = true
                },
            )
        }

        if (selectedRuleIds.isNotEmpty() && !sortMode && !editMode) {
            RuleSelectionBar(
                selectedCount = selectedRuleIds.size,
                allSelected = selectedRuleIds.size == categoryRules.size,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 12.dp, top = topContentPadding, end = 12.dp),
                onCloseClick = { selectedRuleIds = emptySet() },
                onSelectAllClick = {
                    selectedRuleIds = if (selectedRuleIds.size == categoryRules.size) {
                        emptySet()
                    } else {
                        categoryRuleIds
                    }
                },
                onDeleteClick = { showDeleteDialog = true },
            )
        }

        if (selectedCategory != RulePageCategory.System && !sortMode && !selectionMode) {
            AddRuleMenu(
                category = selectedCategory,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = bottomContentPadding + 24.dp),
                onBrowserClick = { context.startActivity(Intent(context, RuleBrowserActivity::class.java)) },
                onLinkRuleClick = {
                    context.startActivity(
                        Intent(context, RuleEditorActivity::class.java)
                            .putExtra(RuleEditorActivity.EXTRA_CATEGORY, RuleCategory.Link.value),
                    )
                },
                onExpressRuleClick = {
                    context.startActivity(
                        Intent(context, RuleEditorActivity::class.java)
                            .putExtra(RuleEditorActivity.EXTRA_CATEGORY, RuleCategory.Express.value),
                    )
                },
            )
        }

        resolvingUrl?.let { url ->
            HiddenWebViewResolver(
                url = url,
                onResolved = { resolvedUrl ->
                    resolvingUrl = null
                    val rule = resolvingRule
                    resolvingRule = null
                    val intent = RuleTarget(
                        type = if (resolvedUrl.startsWith("intent://", true)) RuleTargetType.Intent else RuleTargetType.Url,
                        template = resolvedUrl,
                        packageName = if (resolvedUrl.startsWith("intent://", true)) "" else rule?.target?.packageName.orEmpty(),
                    ).toIntent(emptyMap())
                    resultText = context.getString(R.string.rule_result_webview_resolved, resolvedUrl)
                    runCatching { context.startActivity(intent) }
                        .onFailure { resultText = context.getString(R.string.rule_result_launch_failed, it.message) }
                },
                onTimeout = {
                    val rule = resolvingRule
                    resolvingUrl = null
                    resolvingRule = null
                    if (rule == null) {
                        resultText = context.getString(R.string.rule_result_webview_no_jump)
                    } else {
                        val intent = RuleTarget(
                            type = RuleTargetType.Url,
                            template = url,
                            packageName = rule.target.packageName,
                        ).toIntent(emptyMap())
                        resultText = context.getString(R.string.rule_result_webview_fallback, intent.data)
                        runCatching { context.startActivity(intent) }
                            .onFailure { resultText = context.getString(R.string.rule_result_launch_failed, it.message) }
                    }
                },
                onPageLoaded = {
                    Toast.makeText(context, R.string.rule_toast_page_loaded, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(320.dp),
            )
        }

        WindowDialog(
            title = stringResource(R.string.rule_dialog_delete_title),
            summary = stringResource(R.string.rule_dialog_delete_summary, selectedRuleIds.size),
            show = showDeleteDialog,
            onDismissRequest = { showDeleteDialog = false },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.action_confirm),
                    onClick = {
                        repository.deleteRules(selectedRuleIds)
                        selectedRuleIds = emptySet()
                        rules = repository.readRules()
                        showDeleteDialog = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(textColor = Color(0xFFFF5A52)),
                )
            }
        }
    }

    WindowDialog(
        title = stringResource(R.string.rule_dialog_import_title),
        summary = stringResource(R.string.rule_dialog_import_summary),
        show = showImportDialog,
        onDismissRequest = onDismissImportDialog,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = importText,
                onValueChange = { importText = it },
                label = stringResource(R.string.rule_dialog_import_hint),
                maxLines = 15,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismissImportDialog,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.action_import_rule),
                    onClick = {
                        runCatching {
                            val importedRules = rulesFromJson(importText)
                            if (importedRules.isEmpty()) error(context.getString(R.string.rule_import_empty))
                            val importedIds = importedRules.map { it.id }.toSet()
                            repository.persistRules(repository.readRules().filterNot { it.id in importedIds } + importedRules)
                            rules = repository.readRules()
                            importText = ""
                            onDismissImportDialog()
                            Toast.makeText(context, context.getString(R.string.rule_toast_imported, importedRules.size), Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, context.getString(R.string.rule_toast_import_failed, it.message.orEmpty()), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

private fun toggleSystemLinkDomain(
    context: android.content.Context,
    repository: SystemLinkRepository,
    userId: Int,
    app: SystemLinkApp,
    host: String,
    enabled: Boolean,
    onReloaded: (List<SystemLinkApp>) -> Unit,
) {
    thread(name = "HyperCopySystemLinkToggle") {
        if (!Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+").matches(app.packageName)) {
            HyperLog.d("HyperCopy", "invalid system link package: ${app.packageName}")
            return@thread
        }
        runCatching { repository.setDomainEnabled(userId, app.packageName, host, enabled) }
            .onFailure { HyperLog.d("HyperCopy", "toggle system link failed", it) }
        val apps = runCatching { repository.readApps(userId) }
            .getOrElse { throwable ->
                HyperLog.d("HyperCopy", "reload system links failed", throwable)
                listOf(app)
            }
        (context as? android.app.Activity)?.runOnUiThread { onReloaded(apps) }
    }
}

private fun toggleSystemLinkApp(
    context: android.content.Context,
    repository: SystemLinkRepository,
    userId: Int,
    app: SystemLinkApp,
    enabled: Boolean,
    onReloaded: (List<SystemLinkApp>) -> Unit,
) {
    thread(name = "HyperCopySystemLinkAppToggle") {
        runCatching { repository.setLinkHandlingAllowed(userId, app.packageName, enabled) }
            .onFailure { HyperLog.d("HyperCopy", "toggle app system link failed", it) }
        val apps = runCatching { repository.readApps(userId) }
            .getOrElse { throwable ->
                HyperLog.d("HyperCopy", "reload system links failed", throwable)
                listOf(app)
            }
        (context as? android.app.Activity)?.runOnUiThread { onReloaded(apps) }
    }
}

private fun executeRuleTest(
    context: android.content.Context,
    input: String,
    rules: List<RuleConfig>,
    category: RulePageCategory,
    onStartWebViewResolve: (String, RuleConfig) -> Unit,
    onStartRedirectParse: (String, RuleConfig) -> Unit,
    onStartActivity: (Intent) -> Unit,
): String {
    val value = input.trim()
    val categoryTitle = context.getString(category.titleRes())
    if (value.isBlank()) return context.getString(R.string.rule_result_input_required, categoryTitle)
    val rule = findRule(value, rules) ?: return context.getString(R.string.rule_result_no_match, categoryTitle)
    return when (rule.actionMode) {
        RuleActionMode.ParseAndOpen -> {
            val match = matchRule(value, listOf(rule)) ?: return context.getString(R.string.rule_result_match_no_param, rule.name)
            runCatching { onStartActivity(match.intent) }
                .fold(
                    onSuccess = { context.getString(R.string.rule_result_match_parse_open, rule.name, match.intent.data) },
                    onFailure = { context.getString(R.string.rule_result_launch_failed, it.message) },
                )
        }

        RuleActionMode.DirectOpen -> {
            val intent = rule.directIntent(value, context.packageManager)
            runCatching { onStartActivity(intent) }
                .fold(
                    onSuccess = { context.getString(R.string.rule_result_match_direct_open, rule.name, intent.data) },
                    onFailure = { context.getString(R.string.rule_result_launch_failed, it.message) },
                )
        }

        RuleActionMode.WebViewResolveAndOpen -> {
            val resolveUrl = rule.resolveInputUrl(value)
            if (rule.parseAfterRedirect) {
                onStartRedirectParse(resolveUrl, rule)
                return context.getString(R.string.rule_result_match_redirect_parse, rule.name)
            }
            onStartWebViewResolve(resolveUrl, rule)
            context.getString(R.string.rule_result_match_webview, rule.name)
        }
    }
}
