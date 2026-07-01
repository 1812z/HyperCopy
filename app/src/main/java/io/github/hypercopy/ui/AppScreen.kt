package io.github.hypercopy.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.hypercopy.App
import io.github.hypercopy.R
import io.github.hypercopy.clipboard.monitor.ClipboardMonitorController
import io.github.hypercopy.data.SettingsRepository
import io.github.hypercopy.ui.rules.CloudRulesPage
import io.github.hypercopy.ui.rules.RulesPage
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AppRecording
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Carrier
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class Tab(val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelRes: Int) {
    Home(MiuixIcons.Backup, R.string.tab_home),
    Copy(MiuixIcons.Carrier, R.string.tab_cloud_rules),
    Rules(MiuixIcons.AppRecording, R.string.tab_rules),
    Settings(MiuixIcons.Settings, R.string.tab_settings),
}

private enum class SettingsDestination {
    Main,
    Theme,
    AppList,
}

@Composable
fun AppScreen(
    colorMode: AppColorMode = AppColorMode.System,
    onColorModeChange: (AppColorMode) -> Unit = {},
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context.applicationContext) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    val tabs = remember { Tab.entries.toList() }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(Tab.Home) }
    var settingsDestination by remember { mutableStateOf(SettingsDestination.Main) }
    var xposedService by remember { mutableStateOf(App.xposedService) }
    var logLevel by remember { mutableIntStateOf(settingsRepository.readLogLevel()) }
    var autoCheckUpdate by remember { mutableStateOf(settingsRepository.readAutoCheckUpdate()) }
    var desktopIconHidden by remember { mutableStateOf(settingsRepository.readDesktopIconHidden()) }
    var appLanguage by remember { mutableStateOf(appLanguageFromValue(settingsRepository.readAppLanguage())) }
    var clipboardMonitorMode by remember {
        mutableStateOf(clipboardMonitorModeFromValue(settingsRepository.readClipboardMonitorMode()))
    }
    var jumpNotificationMode by remember {
        mutableStateOf(jumpNotificationModeFromValue(settingsRepository.readJumpNotificationMode()))
    }
    var appListWorkMode by remember { mutableStateOf(settingsRepository.readAppListWorkMode()) }
    var ignoreJumpApp by remember { mutableStateOf(settingsRepository.readIgnoreJumpApp()) }

    DisposableEffect(Unit) {
        val listener: (XposedService?) -> Unit = { service -> xposedService = service }
        App.addServiceListener(listener)
        onDispose { App.removeServiceListener(listener) }
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = tabs[pagerState.currentPage]
        if (selectedTab != Tab.Settings) settingsDestination = SettingsDestination.Main
    }

    BackHandler(enabled = selectedTab == Tab.Settings && settingsDestination != SettingsDestination.Main) {
        settingsDestination = SettingsDestination.Main
    }

    val backgroundColor = appBackground(colorMode)

    Scaffold(
        bottomBar = {
            BottomNavigation(tabs, selectedTab) { index, tab ->
                selectedTab = tab
                settingsDestination = SettingsDestination.Main
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (tabs[page]) {
                    Tab.Home -> HomePage(
                        xposedService = xposedService,
                        clipboardMonitorMode = clipboardMonitorMode,
                        onClipboardMonitorModeChange = {
                            clipboardMonitorMode = it
                            settingsRepository.persistClipboardMonitorMode(it.value)
                            ClipboardMonitorController.onModeChanged(context.applicationContext, it.value)
                        },
                        bottomContentPadding = 16.dp,
                    )
                    Tab.Copy -> CloudRulesPage(bottomContentPadding = 16.dp)
                    Tab.Rules -> RulesPage(bottomContentPadding = 16.dp)
                    Tab.Settings -> AnimatedContent(
                        targetState = settingsDestination,
                        transitionSpec = {
                            val direction = if (targetState == SettingsDestination.Main) {
                                AnimatedContentTransitionScope.SlideDirection.Right
                            } else {
                                AnimatedContentTransitionScope.SlideDirection.Left
                            }
                            (slideIntoContainer(direction, tween(260)) + fadeIn(tween(160))) togetherWith
                                (slideOutOfContainer(direction, tween(260)) + fadeOut(tween(160)))
                        },
                        label = "SettingsDestination",
                    ) { destination ->
                        when (destination) {
                            SettingsDestination.Main -> SettingsPage(
                                logLevel = logLevel,
                                autoCheckUpdate = autoCheckUpdate,
                                desktopIconHidden = desktopIconHidden,
                                appLanguage = appLanguage,
                                jumpNotificationMode = jumpNotificationMode,
                                onLogLevelChange = {
                                    logLevel = it
                                    settingsRepository.persistLogLevel(it)
                                },
                                onAutoCheckUpdateChange = {
                                    autoCheckUpdate = it
                                    settingsRepository.persistAutoCheckUpdate(it)
                                },
                                onDesktopIconHiddenChange = {
                                    desktopIconHidden = it
                                    settingsRepository.persistDesktopIconHidden(it)
                                },
                                onAppLanguageChange = {
                                    appLanguage = it
                                    settingsRepository.persistAppLanguage(it.value)
                                },
                                onJumpNotificationModeChange = {
                                    jumpNotificationMode = it
                                    settingsRepository.persistJumpNotificationMode(it.value)
                                    if (it != JumpNotificationMode.None && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onCheckUpdate = {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_update_check_unconfigured),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                                onOpenTheme = { settingsDestination = SettingsDestination.Theme },
                                onOpenAppList = { settingsDestination = SettingsDestination.AppList },
                                bottomContentPadding = 16.dp,
                            )

                            SettingsDestination.Theme -> ThemeSettingsPage(
                                colorMode = colorMode,
                                onColorModeChange = {
                                    onColorModeChange(it)
                                    settingsRepository.persistColorMode(it.value)
                                },
                                bottomContentPadding = 16.dp,
                            )

                            SettingsDestination.AppList -> AppListPage(
                                workMode = appListWorkMode,
                                ignoreJumpApp = ignoreJumpApp,
                                onWorkModeChange = {
                                    appListWorkMode = it
                                    settingsRepository.persistAppListWorkMode(it)
                                },
                                onIgnoreJumpAppChange = {
                                    ignoreJumpApp = it
                                    settingsRepository.persistIgnoreJumpApp(it)
                                },
                                onBack = { settingsDestination = SettingsDestination.Main },
                                bottomContentPadding = 16.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigation(
    tabs: List<Tab>,
    selectedTab: Tab,
    onTabClick: (Int, Tab) -> Unit,
) {
    NavigationBar(color = MiuixTheme.colorScheme.surface, showDivider = false) {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabClick(index, tab) },
                icon = tab.icon,
                label = stringResource(tab.labelRes),
            )
        }
    }
}

@Composable
fun appBackground(colorMode: AppColorMode = AppColorMode.System): Color {
    val dark = when (colorMode) {
        AppColorMode.System -> isSystemInDarkTheme()
        AppColorMode.Dark -> true
        AppColorMode.Light -> false
    }
    return if (dark) Color(0xFF101010) else Color(0xFFF5F5F7)
}
