package io.github.hypercopy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hypercopy.Config
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AppRecording
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPage(
    logLevel: Int,
    autoCheckUpdate: Boolean,
    desktopIconHidden: Boolean,
    appLanguage: AppLanguage,
    clipboardMonitorMode: ClipboardMonitorMode,
    onLogLevelChange: (Int) -> Unit,
    onAutoCheckUpdateChange: (Boolean) -> Unit,
    onDesktopIconHiddenChange: (Boolean) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onClipboardMonitorModeChange: (ClipboardMonitorMode) -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenTheme: () -> Unit,
    bottomContentPadding: Dp = 16.dp,
) {
    val uriHandler = LocalUriHandler.current
    val strings = LocalAppStrings.current
    val logLevelOptions = logLevelOptions(strings)
    val languageOptions = languageOptions(strings)
    val clipboardMonitorModeOptions = clipboardMonitorModeOptions(strings)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = strings.settings,
                style = MiuixTheme.textStyles.title1,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item { SmallTitle(text = strings.appearance) }
        item {
            Card {
                OverlayDropdownPreference(
                    title = strings.language,
                    summary = strings.languageSummary,
                    items = languageOptions.map { it.label },
                    selectedIndex = languageOptions.indexOfFirst { it.value == appLanguage }.coerceAtLeast(0),
                    insideMargin = SettingsItemMargin,
                    onSelectedIndexChange = { onAppLanguageChange(languageOptions[it].value) },
                )
                SettingsAction(
                    icon = MiuixIcons.Theme,
                    title = strings.theme,
                    summary = strings.themeSummary,
                    onClick = onOpenTheme,
                )
            }
        }

        item { SmallTitle(text = strings.softwareSettings) }
        item {
            Card {
                OverlayDropdownPreference(
                    title = strings.clipboardMonitorMode,
                    summary = strings.clipboardMonitorModeSummary,
                    items = clipboardMonitorModeOptions.map { it.label },
                    selectedIndex = clipboardMonitorModeOptions.indexOfFirst { it.value == clipboardMonitorMode }.coerceAtLeast(0),
                    insideMargin = SettingsItemMargin,
                    onSelectedIndexChange = { onClipboardMonitorModeChange(clipboardMonitorModeOptions[it].value) },
                )
                OverlayDropdownPreference(
                    title = strings.logLevel,
                    summary = strings.logLevelSummary,
                    items = logLevelOptions.map { it.label },
                    selectedIndex = logLevelOptions.indexOfFirst { it.value == logLevel }.coerceAtLeast(0),
                    insideMargin = SettingsItemMargin,
                    onSelectedIndexChange = { onLogLevelChange(logLevelOptions[it].value) },
                )
                SettingsAction(
                    icon = MiuixIcons.Download,
                    title = strings.checkUpdate,
                    summary = strings.checkUpdateSummary,
                    onClick = onCheckUpdate,
                )
                SwitchAction(
                    icon = MiuixIcons.Update,
                    title = strings.autoCheckUpdate,
                    summary = strings.autoCheckUpdateSummary,
                    checked = autoCheckUpdate,
                    onCheckedChange = { onAutoCheckUpdateChange(!autoCheckUpdate) },
                )
                SwitchAction(
                    icon = MiuixIcons.AppRecording,
                    title = strings.hideDesktopIcon,
                    summary = strings.hideDesktopIconSummary,
                    checked = desktopIconHidden,
                    onCheckedChange = { onDesktopIconHiddenChange(!desktopIconHidden) },
                )
            }
        }

        item { SmallTitle(text = strings.about) }
        item {
            Card {
                SettingsAction(
                    icon = MiuixIcons.Link,
                    title = strings.openHomePage,
                    summary = strings.openHomePageSummary,
                    onClick = { uriHandler.openUri(GITHUB_URL) },
                )
            }
        }
    }
}

@Composable
fun SettingsAction(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    SettingsRow(icon = icon, title = title, summary = summary, role = Role.Button, onClick = onClick)
}

@Composable
fun SwitchAction(icon: ImageVector, title: String, summary: String, checked: Boolean, onCheckedChange: () -> Unit) {
    SettingsRow(
        icon = icon,
        title = title,
        summary = summary,
        role = Role.Switch,
        onClick = onCheckedChange,
        trailing = { Switch(checked = checked, onCheckedChange = { onCheckedChange() }) },
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    summary: String,
    role: Role,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = role, onClick = onClick).padding(SettingsItemMargin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        SettingsText(
            title = title,
            summary = summary,
            modifier = if (trailing == null) {
                Modifier.padding(start = SettingsTextStartPadding)
            } else {
                Modifier.padding(start = SettingsTextStartPadding, end = 12.dp).weight(1f)
            },
        )
        trailing?.invoke()
    }
}

@Composable
fun SettingsIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurface,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun SettingsText(title: String, summary: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = title, style = MiuixTheme.textStyles.headline1)
        Text(
            text = summary,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private data class LogLevelOption(val label: String, val value: Int)

private data class LanguageOption(val label: String, val value: AppLanguage)

private data class ClipboardMonitorModeOption(val label: String, val value: ClipboardMonitorMode)

private fun logLevelOptions(strings: UiStrings) = listOf(
    LogLevelOption(strings.logOff, Config.LOG_LEVEL_OFF),
    LogLevelOption(strings.logBasic, Config.LOG_LEVEL_BASIC),
    LogLevelOption(strings.logDebug, Config.LOG_LEVEL_DEBUG),
)

private fun languageOptions(strings: UiStrings) = listOf(
    LanguageOption(strings.languageChinese, AppLanguage.Chinese),
)

private fun clipboardMonitorModeOptions(strings: UiStrings) = listOf(
    ClipboardMonitorModeOption(strings.clipboardMonitorModeLSPosed, ClipboardMonitorMode.LSPosed),
)

private val SettingsItemMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
private val SettingsTextStartPadding = 16.dp
private const val GITHUB_URL = "https://github.com/1812z/HyperCopy"
