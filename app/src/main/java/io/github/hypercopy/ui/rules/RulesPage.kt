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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.hypercopy.data.RuleActionMode
import io.github.hypercopy.data.RuleCategory
import io.github.hypercopy.data.RuleConfig
import io.github.hypercopy.data.RuleRepository
import io.github.hypercopy.data.RuleTarget
import io.github.hypercopy.data.RuleTargetType
import io.github.hypercopy.data.directIntent
import io.github.hypercopy.data.findRule
import io.github.hypercopy.data.matchRule
import io.github.hypercopy.data.toIntent
import io.github.hypercopy.ui.HiddenWebViewResolver
import io.github.hypercopy.ui.LocalAppStrings
import io.github.hypercopy.ui.RuleBrowserActivity
import io.github.hypercopy.ui.RuleEditorActivity
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RulesPage(bottomContentPadding: Dp = 16.dp) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember { RuleRepository(context.applicationContext) }
    var rules by remember { mutableStateOf(repository.readRules()) }
    var selectedCategory by remember { mutableStateOf(RulePageCategory.Link) }
    var testInput by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("等待测试") }
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
                    text = LocalAppStrings.current.rules,
                    style = MiuixTheme.textStyles.title1,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                RuleCategoryTabs(
                    selectedCategory = selectedCategory,
                    onSelected = {
                        selectedCategory = it
                        resultText = "等待测试"
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
                            input = testInput,
                            rules = categoryRules,
                            category = selectedCategory,
                            onStartWebViewResolve = { url, rule ->
                                resolvingUrl = url
                                resolvingRule = rule
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
                    resultText = "WebView 解析成功：$resolvedUrl"
                    runCatching { context.startActivity(intent) }
                        .onFailure { resultText = "启动失败：${it.message}" }
                },
                onTimeout = {
                    val rule = resolvingRule
                    resolvingUrl = null
                    resolvingRule = null
                    if (rule == null) {
                        resultText = "WebView 未解析到跳转"
                    } else {
                        val intent = rule.directIntent(url)
                        resultText = "WebView 未解析到跳转，改为直接打开：${intent.data}"
                        runCatching { context.startActivity(intent) }
                            .onFailure { resultText = "启动失败：${it.message}" }
                    }
                },
                onPageLoaded = {
                    Toast.makeText(context, "页面已加载，如无跳转可改用直接打开模式", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(320.dp),
            )
        }

        OverlayDialog(
            title = "删除规则",
            summary = "确定删除已选择的 ${selectedRuleIds.size} 项规则？",
            show = showDeleteDialog,
            onDismissRequest = { showDeleteDialog = false },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = "取消",
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "确定",
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
    input: String,
    rules: List<RuleConfig>,
    category: RulePageCategory,
    onStartWebViewResolve: (String, RuleConfig) -> Unit,
    onStartActivity: (Intent) -> Unit,
): String {
    val value = input.trim()
    if (value.isBlank()) return "请输入要测试的${category.title()}内容"
    val rule = findRule(value, rules) ?: return "未命中${category.title()}规则"
    return when (rule.actionMode) {
        RuleActionMode.ParseAndOpen -> {
            val match = matchRule(value, listOf(rule)) ?: return "命中 ${rule.name}，但参数正则没有提取到内容"
            runCatching { onStartActivity(match.intent) }
                .fold(
                    onSuccess = { "命中 ${rule.name}，解析参数打开：${match.intent.data}" },
                    onFailure = { "启动失败：${it.message}" },
                )
        }

        RuleActionMode.DirectOpen -> {
            val intent = rule.directIntent(value)
            runCatching { onStartActivity(intent) }
                .fold(
                    onSuccess = { "命中 ${rule.name}，直接打开：${intent.data}" },
                    onFailure = { "启动失败：${it.message}" },
                )
        }

        RuleActionMode.WebViewResolveAndOpen -> {
            onStartWebViewResolve(normalizeTestUrl(value), rule)
            "命中 ${rule.name}，正在用 WebView 模拟打开。页面加载后会通用点击一次，2 秒无跳转则直接打开目标 App。"
        }
    }
}

private fun normalizeTestUrl(text: String): String {
    val value = text.trim()
    val uri = runCatching { Uri.parse(value) }.getOrNull()
    return if (uri?.scheme.isNullOrBlank()) "https://$value" else value
}
