package io.github.hypercopy.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.hypercopy.data.RuleActionMode
import io.github.hypercopy.data.RuleCategory
import io.github.hypercopy.data.RuleConfig
import io.github.hypercopy.data.RuleRepository
import io.github.hypercopy.data.RuleTarget
import io.github.hypercopy.data.RuleTargetType
import io.github.hypercopy.data.SettingsRepository
import io.github.hypercopy.data.ruleCategoryFromValue
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class RuleEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val colorMode = remember { appColorModeFromValue(settingsRepository.readColorMode()) }
            MiuixTheme(controller = ThemeController(colorMode.toColorSchemeMode())) {
                RuleEditorScreen(
                    ruleId = intent.getStringExtra(EXTRA_RULE_ID).orEmpty(),
                    initialCategory = ruleCategoryFromValue(intent.getStringExtra(EXTRA_CATEGORY).orEmpty()),
                    initialSourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL).orEmpty(),
                    initialTargetUrl = intent.getStringExtra(EXTRA_TARGET_URL).orEmpty(),
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_SOURCE_URL = "source_url"
        const val EXTRA_TARGET_URL = "target_url"
        const val EXTRA_RULE_ID = "rule_id"
        const val EXTRA_CATEGORY = "category"
    }
}

@Composable
private fun RuleEditorScreen(
    ruleId: String,
    initialCategory: RuleCategory,
    initialSourceUrl: String,
    initialTargetUrl: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { RuleRepository(context.applicationContext) }
    val editingRule = remember(ruleId) { repository.readRules().firstOrNull { it.id == ruleId } }
    val category = editingRule?.category ?: initialCategory
    val defaults = remember(category, initialSourceUrl, initialTargetUrl) {
        defaultEditorValues(category, initialSourceUrl, initialTargetUrl)
    }
    var name by remember { mutableStateOf(editingRule?.name ?: defaults.name) }
    var sourceUrl by remember { mutableStateOf(editingRule?.sourceUrl ?: defaults.sourceUrl) }
    var matchRegex by remember { mutableStateOf(editingRule?.matchRegex ?: defaults.matchRegex) }
    var parameterRegex by remember { mutableStateOf(editingRule?.parameterRegex ?: defaults.parameterRegex) }
    var targetTemplate by remember { mutableStateOf(editingRule?.target?.template ?: defaults.targetTemplate) }
    var packageName by remember { mutableStateOf(editingRule?.target?.packageName ?: defaults.packageName) }
    var openMode by remember { mutableStateOf(openModeFromRule(editingRule, category)) }
    var actionMode by remember { mutableStateOf(editingRule?.actionMode ?: defaults.actionMode) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(modifier = Modifier.size(42.dp), onClick = onBack) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                    }
                }
                Text(text = if (editingRule == null) "添加${category.label()}规则" else "编辑${category.label()}规则", style = MiuixTheme.textStyles.title1)
            }

            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(value = name, onValueChange = { name = it }, label = "名称", singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (category == RuleCategory.Link) {
                        TextField(value = sourceUrl, onValueChange = { sourceUrl = it }, label = "来源 URL", maxLines = 3, modifier = Modifier.fillMaxWidth())
                    }
                    TextField(value = matchRegex, onValueChange = { matchRegex = it }, label = "匹配正则", maxLines = 3, modifier = Modifier.fillMaxWidth())
                    if (category == RuleCategory.Link) {
                        TextField(value = parameterRegex, onValueChange = { parameterRegex = it }, label = "参数正则", maxLines = 3, modifier = Modifier.fillMaxWidth())
                    }
                    TextField(value = targetTemplate, onValueChange = { targetTemplate = it }, label = if (category == RuleCategory.Link) "跳转模板" else "打开内容模板", maxLines = 3, modifier = Modifier.fillMaxWidth())
                    if (category == RuleCategory.Link) {
                        TextField(value = packageName, onValueChange = { packageName = it }, label = "包名，可留空", singleLine = true, modifier = Modifier.fillMaxWidth())
                        ActionModeSelector(selected = actionMode, onSelected = { actionMode = it })
                    } else {
                        CategoryOpenModeSelector(selected = openMode, onSelected = { openMode = it })
                        if (openMode == CategoryOpenMode.DirectApp) {
                            TextField(value = packageName, onValueChange = { packageName = it }, label = "目标 App 包名，可留空", singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(
                        text = "保存规则",
                        onClick = {
                            val rule = RuleConfig(
                                id = editingRule?.id ?: ruleId.ifBlank { java.util.UUID.randomUUID().toString() },
                                name = name.ifBlank { "未命名规则" },
                                category = category,
                                actionMode = if (category == RuleCategory.Link) actionMode else RuleActionMode.DirectOpen,
                                matchRegex = matchRegex.ifBlank { ".*" },
                                parameterRegex = parameterRegex.ifBlank { ".*(.+).*" },
                                target = RuleTarget(
                                    type = if (targetTemplate.startsWith("intent://", true)) RuleTargetType.Intent else RuleTargetType.Url,
                                    template = targetTemplate,
                                    packageName = if (category == RuleCategory.Link || openMode == CategoryOpenMode.DirectApp) packageName else "",
                                ),
                                sourceUrl = if (category == RuleCategory.Link) sourceUrl else "",
                            )
                            repository.saveRule(rule)
                            Toast.makeText(context, "规则已保存", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }
}

private data class EditorDefaults(
    val name: String,
    val sourceUrl: String,
    val matchRegex: String,
    val parameterRegex: String,
    val targetTemplate: String,
    val packageName: String,
    val actionMode: RuleActionMode,
)

private enum class CategoryOpenMode {
    DirectApp,
    Url,
}

private val linkActionTabs = listOf("解析参数", "直接打开", "模拟打开")
private val categoryOpenModeTabs = listOf("直接打开 App", "打开 URL")

private fun defaultEditorValues(category: RuleCategory, sourceUrl: String, targetUrl: String): EditorDefaults = when (category) {
    RuleCategory.Link -> EditorDefaults(
        name = ruleNameFromTarget(targetUrl),
        sourceUrl = sourceUrl,
        matchRegex = if (sourceUrl.isBlank()) ".*" else ".*${Regex.escape(sourceUrl)}.*",
        parameterRegex = ".*(.+).*",
        targetTemplate = targetUrl.ifBlank { "${'$'}{input}" },
        packageName = parsePackageName(targetUrl),
        actionMode = if (sourceUrl.isNotBlank()) RuleActionMode.WebViewResolveAndOpen else RuleActionMode.DirectOpen,
    )

    RuleCategory.Address -> EditorDefaults(
        name = "地址规则",
        sourceUrl = "",
        matchRegex = "(?=.*(地址|省|市|区)).{10,}",
        parameterRegex = "(.+)",
        targetTemplate = "${'$'}{input}",
        packageName = "",
        actionMode = RuleActionMode.DirectOpen,
    )

    RuleCategory.Express -> EditorDefaults(
        name = "",
        sourceUrl = "",
        matchRegex = "",
        parameterRegex = "",
        targetTemplate = "",
        packageName = "",
        actionMode = RuleActionMode.DirectOpen,
    )
}

private fun openModeFromRule(rule: RuleConfig?, category: RuleCategory): CategoryOpenMode {
    if (category == RuleCategory.Link) return CategoryOpenMode.DirectApp
    return if (rule?.target?.packageName.isNullOrBlank()) CategoryOpenMode.Url else CategoryOpenMode.DirectApp
}

private fun RuleCategory.label(): String = when (this) {
    RuleCategory.Link -> "链接"
    RuleCategory.Address -> "地址"
    RuleCategory.Express -> "快递"
}

@Composable
private fun ActionModeSelector(selected: RuleActionMode, onSelected: (RuleActionMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "执行类型", style = MiuixTheme.textStyles.headline1)
        TabRowWithContour(
            tabs = linkActionTabs,
            selectedTabIndex = selected.tabIndex(),
            onTabSelected = { onSelected(ruleActionModeFromTab(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CategoryOpenModeSelector(selected: CategoryOpenMode, onSelected: (CategoryOpenMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "打开方式", style = MiuixTheme.textStyles.headline1)
        TabRowWithContour(
            tabs = categoryOpenModeTabs,
            selectedTabIndex = selected.tabIndex(),
            onTabSelected = { onSelected(categoryOpenModeFromTab(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun RuleActionMode.tabIndex(): Int = when (this) {
    RuleActionMode.ParseAndOpen -> 0
    RuleActionMode.DirectOpen -> 1
    RuleActionMode.WebViewResolveAndOpen -> 2
}

private fun ruleActionModeFromTab(index: Int): RuleActionMode = when (index) {
    1 -> RuleActionMode.DirectOpen
    2 -> RuleActionMode.WebViewResolveAndOpen
    else -> RuleActionMode.ParseAndOpen
}

private fun CategoryOpenMode.tabIndex(): Int = when (this) {
    CategoryOpenMode.DirectApp -> 0
    CategoryOpenMode.Url -> 1
}

private fun categoryOpenModeFromTab(index: Int): CategoryOpenMode = when (index) {
    1 -> CategoryOpenMode.Url
    else -> CategoryOpenMode.DirectApp
}

private fun ruleNameFromTarget(targetUrl: String): String {
    val uri = runCatching { Uri.parse(targetUrl) }.getOrNull()
    return when {
        uri?.scheme?.isNotBlank() == true -> "${uri.scheme} 跳转"
        else -> "新规则"
    }
}

private fun parsePackageName(targetUrl: String): String {
    return runCatching { Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME).`package`.orEmpty() }.getOrDefault("")
}

private fun AppColorMode.toColorSchemeMode(): ColorSchemeMode = when (this) {
    AppColorMode.System -> ColorSchemeMode.System
    AppColorMode.Light -> ColorSchemeMode.Light
    AppColorMode.Dark -> ColorSchemeMode.Dark
}
