package io.github.hypercopy.ui

import android.content.Intent
import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.hypercopy.R
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
        defaultEditorValues(context, category, initialSourceUrl, initialTargetUrl)
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
                        Icon(imageVector = MiuixIcons.Back, contentDescription = stringResource(R.string.action_back))
                    }
                }
                Text(
                    text = stringResource(
                        if (editingRule == null) R.string.editor_title_add else R.string.editor_title_edit,
                        stringResource(category.labelRes()),
                    ),
                    style = MiuixTheme.textStyles.title1,
                )
            }

            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.editor_label_name), singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (category == RuleCategory.Link) {
                        TextField(value = sourceUrl, onValueChange = { sourceUrl = it }, label = stringResource(R.string.editor_label_source_url), maxLines = 3, modifier = Modifier.fillMaxWidth())
                    }
                    TextField(value = matchRegex, onValueChange = { matchRegex = it }, label = stringResource(R.string.editor_label_match_regex), maxLines = 3, modifier = Modifier.fillMaxWidth())
                    if (category == RuleCategory.Link) {
                        TextField(value = parameterRegex, onValueChange = { parameterRegex = it }, label = stringResource(R.string.editor_label_parameter_regex), maxLines = 3, modifier = Modifier.fillMaxWidth())
                    }
                    TextField(
                        value = targetTemplate,
                        onValueChange = { targetTemplate = it },
                        label = stringResource(if (category == RuleCategory.Link) R.string.editor_label_target_template else R.string.editor_label_open_content_template),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (category == RuleCategory.Link) {
                        TextField(value = packageName, onValueChange = { packageName = it }, label = stringResource(R.string.editor_label_package_name_optional), singleLine = true, modifier = Modifier.fillMaxWidth())
                        ActionModeSelector(selected = actionMode, onSelected = { actionMode = it })
                    } else {
                        CategoryOpenModeSelector(selected = openMode, onSelected = { openMode = it })
                        if (openMode == CategoryOpenMode.DirectApp) {
                            TextField(value = packageName, onValueChange = { packageName = it }, label = stringResource(R.string.editor_label_target_app_package_optional), singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(
                        text = stringResource(R.string.action_save_rule),
                        onClick = {
                            val rule = RuleConfig(
                                id = editingRule?.id ?: ruleId.ifBlank { java.util.UUID.randomUUID().toString() },
                                name = name.ifBlank { context.getString(R.string.rule_unnamed) },
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
                            Toast.makeText(context, R.string.rule_toast_saved, Toast.LENGTH_SHORT).show()
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

@Composable
private fun linkActionTabs() = listOf(
    stringResource(R.string.editor_link_action_parse),
    stringResource(R.string.editor_link_action_direct),
    stringResource(R.string.editor_link_action_webview),
)

@Composable
private fun categoryOpenModeTabs() = listOf(
    stringResource(R.string.editor_open_app),
    stringResource(R.string.editor_open_url),
)

private fun defaultEditorValues(context: Context, category: RuleCategory, sourceUrl: String, targetUrl: String): EditorDefaults = when (category) {
    RuleCategory.Link -> EditorDefaults(
        name = ruleNameFromTarget(context, targetUrl),
        sourceUrl = sourceUrl,
        matchRegex = if (sourceUrl.isBlank()) ".*" else ".*${Regex.escape(sourceUrl)}.*",
        parameterRegex = ".*(.+).*",
        targetTemplate = targetUrl.ifBlank { "${'$'}{input}" },
        packageName = parsePackageName(targetUrl),
        actionMode = if (sourceUrl.isNotBlank()) RuleActionMode.WebViewResolveAndOpen else RuleActionMode.DirectOpen,
    )

    RuleCategory.Address -> EditorDefaults(
        name = context.getString(R.string.editor_default_address_name),
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

private fun RuleCategory.labelRes(): Int = when (this) {
    RuleCategory.Link -> R.string.category_link
    RuleCategory.Address -> R.string.category_address
    RuleCategory.Express -> R.string.category_express
}

@Composable
private fun ActionModeSelector(selected: RuleActionMode, onSelected: (RuleActionMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.editor_action_mode), style = MiuixTheme.textStyles.headline1)
        TabRowWithContour(
            tabs = linkActionTabs(),
            selectedTabIndex = selected.tabIndex(),
            onTabSelected = { onSelected(ruleActionModeFromTab(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CategoryOpenModeSelector(selected: CategoryOpenMode, onSelected: (CategoryOpenMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.editor_open_mode), style = MiuixTheme.textStyles.headline1)
        TabRowWithContour(
            tabs = categoryOpenModeTabs(),
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

private fun ruleNameFromTarget(context: Context, targetUrl: String): String {
    val uri = runCatching { Uri.parse(targetUrl) }.getOrNull()
    return when {
        uri?.scheme?.isNotBlank() == true -> context.getString(R.string.editor_rule_name_from_scheme, uri.scheme)
        else -> context.getString(R.string.editor_rule_name_new)
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
