package io.github.hypercopy.ui.rules

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hypercopy.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.hypercopy.clipboard.OneRedirectResolver
import io.github.hypercopy.data.RuleActionMode
import io.github.hypercopy.data.RuleCategory
import io.github.hypercopy.data.RuleConfig
import io.github.hypercopy.data.RuleRepository
import io.github.hypercopy.data.RuleTarget
import io.github.hypercopy.data.RuleTargetType
import io.github.hypercopy.data.directIntent
import io.github.hypercopy.data.findRule
import io.github.hypercopy.data.matchRule
import io.github.hypercopy.data.parseIntent
import io.github.hypercopy.data.toIntent
import io.github.hypercopy.ui.HiddenWebViewResolver
import io.github.hypercopy.ui.RuleBrowserActivity
import io.github.hypercopy.ui.RuleEditorActivity
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.concurrent.thread

@Composable
fun RulesPage(bottomContentPadding: Dp = 16.dp) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember { RuleRepository(context.applicationContext) }
    var rules by remember { mutableStateOf(repository.readRules()) }
    var selectedCategory by remember { mutableStateOf(RulePageCategory.Link) }
    var testInput by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf(context.getString(R.string.rule_result_waiting)) }
    var resolvingUrl by remember { mutableStateOf<String?>(null) }
    var resolvingRule by remember { mutableStateOf<RuleConfig?>(null) }
    var selectedRuleIds by remember { mutableStateOf(emptySet<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, repository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) rules = repository.readRules()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val categoryRules = rules.filter { it.category in selectedCategory.ruleCategories() }
    val categoryRuleIds = categoryRules.map { it.id }.toSet()
    val selectionMode = selectedRuleIds.isNotEmpty()

    BackHandler(enabled = selectionMode && !showDeleteDialog) {
        selectedRuleIds = emptySet()
    }

    LaunchedEffect(selectedCategory, categoryRuleIds) {
        selectedRuleIds = selectedRuleIds.intersect(categoryRuleIds)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = bottomContentPadding + 84.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.tab_rules),
                    style = MiuixTheme.textStyles.title1,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                RuleCategoryTabs(
                    selectedCategory = selectedCategory,
                    onSelected = {
                        selectedCategory = it
                        resultText = context.getString(R.string.rule_result_waiting)
                        selectedRuleIds = emptySet()
                    },
                )
            }
            item {
                TestRuleCard(
                    category = selectedCategory,
                    value = testInput,
                    resultText = resultText,
                    onValueChange = { testInput = it },
                    onExecute = {
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
                    },
                )
            }
            if (categoryRules.isEmpty()) {
                item { EmptyRulesCard(selectedCategory) }
            } else {
                if (selectionMode) {
                    item {
                        RuleSelectionBar(
                            selectedCount = selectedRuleIds.size,
                            allSelected = selectedRuleIds.size == categoryRules.size,
                            onCloseClick = { selectedRuleIds = emptySet() },
                            onSelectAllClick = {
                                selectedRuleIds = if (selectedRuleIds.size == categoryRules.size) {
                                    emptySet()
                                } else {
                                    categoryRuleIds
                                }
                            },
                            onDeleteClick = {
                                showDeleteDialog = true
                            },
                        )
                    }
                }
                items(categoryRules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        selected = rule.id in selectedRuleIds,
                        selectionMode = selectionMode,
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
                    )
                }
            }
        }

        AddRuleMenu(
            category = selectedCategory,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = bottomContentPadding + 24.dp),
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
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(320.dp),
            )
        }

        OverlayDialog(
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
            if (rule.parseAfterRedirect) {
                onStartRedirectParse(normalizeTestUrl(value), rule)
                return context.getString(R.string.rule_result_match_redirect_parse, rule.name)
            }
            onStartWebViewResolve(normalizeTestUrl(value), rule)
            context.getString(R.string.rule_result_match_webview, rule.name)
        }
    }
}

private fun normalizeTestUrl(text: String): String {
    val value = text.trim()
    val uri = runCatching { Uri.parse(value) }.getOrNull()
    return if (uri?.scheme.isNullOrBlank()) "https://$value" else value
}
