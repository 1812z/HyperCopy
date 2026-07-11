package io.github.hypercopy.ui.pages.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hypercopy.Config
import io.github.hypercopy.R
import io.github.hypercopy.ui.framework.AppLanguage
import io.github.hypercopy.ui.framework.ClipboardMonitorMode
import io.github.hypercopy.ui.framework.JumpNotificationMode
import io.github.hypercopy.ui.components.SettingsAction
import io.github.hypercopy.ui.components.SettingsActionWithArrow
import io.github.hypercopy.ui.components.SettingsIcon
import io.github.hypercopy.ui.components.SwitchAction
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.icon.MiuixIcons
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
private const val GITHUB_URL = "https://github.com/1812z/HyperCopy"
