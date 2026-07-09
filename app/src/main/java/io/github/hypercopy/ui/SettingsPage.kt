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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hypercopy.Config
import io.github.hypercopy.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.AppRecording
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Translate
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    logLevel: Int,
    autoCheckUpdate: Boolean,
    desktopIconHidden: Boolean,
    detectClonedApp: Boolean,
    miuiIslandBypassRestriction: Boolean,
    appLanguage: AppLanguage,
    clipboardMonitorMode: ClipboardMonitorMode,
    jumpNotificationMode: JumpNotificationMode,
    onLogLevelChange: (Int) -> Unit,
    onAutoCheckUpdateChange: (Boolean) -> Unit,
    onDesktopIconHiddenChange: (Boolean) -> Unit,
    onDetectClonedAppChange: (Boolean) -> Unit,
    onMiuiIslandBypassRestrictionChange: (Boolean) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onJumpNotificationModeChange: (JumpNotificationMode) -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenAppList: () -> Unit,
    topContentPadding: Dp = 12.dp,
    bottomContentPadding: Dp = 16.dp,
) {
    val uriHandler = LocalUriHandler.current
    val logLevelOptions = logLevelOptions()
    val languageOptions = languageOptions()
    val jumpNotificationModeOptions = jumpNotificationModeOptions()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = topContentPadding, end = 16.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SmallTitle(text = stringResource(R.string.appearance)) }
        item {
            Card {
                OverlayDropdownPreference(
                    title = stringResource(R.string.language),
                    summary = stringResource(R.string.language_summary),
                    items = languageOptions.map { it.label },
                    selectedIndex = languageOptions.indexOfFirst { it.value == appLanguage }.coerceAtLeast(0),
                    startAction = { SettingsIcon(imageVector = MiuixIcons.Translate) },
                    insideMargin = SettingsItemMargin,
                    onSelectedIndexChange = { onAppLanguageChange(languageOptions[it].value) },
                )
                SettingsAction(
                    icon = MiuixIcons.Theme,
                    title = stringResource(R.string.theme),
                    summary = stringResource(R.string.theme_summary),
                    onClick = onOpenTheme,
                )
            }
        }

        item { SmallTitle(text = stringResource(R.string.software_settings)) }
        item {
            Card {
                OverlayDropdownPreference(
                    title = stringResource(R.string.jump_notification_mode),
                    summary = stringResource(R.string.jump_notification_mode_summary),
                    items = jumpNotificationModeOptions.map { it.label },
                    selectedIndex = jumpNotificationModeOptions.indexOfFirst { it.value == jumpNotificationMode }.coerceAtLeast(0),
                    startAction = { SettingsIcon(imageVector = MiuixIcons.Community) },
                    insideMargin = SettingsItemMargin,
                    onSelectedIndexChange = { onJumpNotificationModeChange(jumpNotificationModeOptions[it].value) },
                )
                if (jumpNotificationMode == JumpNotificationMode.MiuiIsland && clipboardMonitorMode == ClipboardMonitorMode.Shizuku) {
                    SwitchAction(
                        icon = MiuixIcons.Community,
                        title = stringResource(R.string.miui_island_bypass_restriction),
                        summary = stringResource(R.string.miui_island_bypass_restriction_summary),
                        checked = miuiIslandBypassRestriction,
                        onCheckedChange = { onMiuiIslandBypassRestrictionChange(!miuiIslandBypassRestriction) },
                    )
                }
                OverlayDropdownPreference(
                    title = stringResource(R.string.log_level),
                    summary = stringResource(R.string.log_level_summary),
                    items = logLevelOptions.map { it.label },
                    selectedIndex = logLevelOptions.indexOfFirst { it.value == logLevel }.coerceAtLeast(0),
                    startAction = { SettingsIcon(imageVector = MiuixIcons.File) },
                    insideMargin = SettingsItemMargin,
                    onSelectedIndexChange = { onLogLevelChange(logLevelOptions[it].value) },
                )
                SettingsActionWithArrow(
                    icon = MiuixIcons.ListView,
                    title = stringResource(R.string.app_list),
                    summary = stringResource(R.string.app_list_summary),
                    onClick = onOpenAppList,
                )
                SettingsAction(
                    icon = MiuixIcons.Download,
                    title = stringResource(R.string.check_update),
                    summary = stringResource(R.string.check_update_summary),
                    onClick = onCheckUpdate,
                )
                SwitchAction(
                    icon = MiuixIcons.Update,
                    title = stringResource(R.string.auto_check_update),
                    summary = stringResource(R.string.auto_check_update_summary),
                    checked = autoCheckUpdate,
                    onCheckedChange = { onAutoCheckUpdateChange(!autoCheckUpdate) },
                )
                SwitchAction(
                    icon = MiuixIcons.Copy,
                    title = stringResource(R.string.detect_cloned_app),
                    summary = stringResource(R.string.detect_cloned_app_summary),
                    checked = detectClonedApp,
                    onCheckedChange = { onDetectClonedAppChange(!detectClonedApp) },
                )
                SwitchAction(
                    icon = MiuixIcons.AppRecording,
                    title = stringResource(R.string.hide_desktop_icon),
                    summary = stringResource(R.string.hide_desktop_icon_summary),
                    checked = desktopIconHidden,
                    onCheckedChange = { onDesktopIconHiddenChange(!desktopIconHidden) },
                )
            }
        }

        item { SmallTitle(text = stringResource(R.string.about)) }
        item {
            Card {
                SettingsAction(
                    icon = MiuixIcons.Link,
                    title = stringResource(R.string.open_home_page),
                    summary = stringResource(R.string.open_home_page_summary),
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
fun SettingsActionWithArrow(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    SettingsRow(
        icon = icon,
        title = title,
        summary = summary,
        role = Role.Button,
        onClick = onClick,
        trailing = {
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(18.dp),
            )
        },
    )
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
    Row(modifier = Modifier.width(32.dp)) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
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

private data class JumpNotificationModeOption(val label: String, val value: JumpNotificationMode)

@Composable
private fun logLevelOptions() = listOf(
    LogLevelOption(stringResource(R.string.log_off), Config.LOG_LEVEL_OFF),
    LogLevelOption(stringResource(R.string.log_basic), Config.LOG_LEVEL_BASIC),
    LogLevelOption(stringResource(R.string.log_debug), Config.LOG_LEVEL_DEBUG),
)

@Composable
private fun languageOptions() = listOf(
    LanguageOption(stringResource(R.string.language_system), AppLanguage.System),
    LanguageOption(stringResource(R.string.language_chinese), AppLanguage.Chinese),
    LanguageOption(stringResource(R.string.language_english), AppLanguage.English),
)

@Composable
private fun jumpNotificationModeOptions() = listOf(
    JumpNotificationModeOption(stringResource(R.string.jump_notification_mode_none), JumpNotificationMode.None),
    // The three jump notification choices shown in Settings.
    JumpNotificationModeOption(stringResource(R.string.jump_notification_mode_normal), JumpNotificationMode.Normal),
    JumpNotificationModeOption(stringResource(R.string.jump_notification_mode_live), JumpNotificationMode.Live),
    JumpNotificationModeOption(stringResource(R.string.jump_notification_mode_miui_island), JumpNotificationMode.MiuiIsland),
)

private val SettingsItemMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
private val SettingsTextStartPadding = 16.dp
private const val GITHUB_URL = "https://github.com/1812z/HyperCopy"
