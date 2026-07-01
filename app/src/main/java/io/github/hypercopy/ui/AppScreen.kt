package io.github.hypercopy.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AppRecording
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Carrier
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class Tab(val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelRes: Int) {
    Home(MiuixIcons.Backup, R.string.tab_home),
    Copy(MiuixIcons.Carrier, R.string.tab_cloud_rules),
    Rules(MiuixIcons.AppRecording, R.string.tab_rules),
    Settings(MiuixIcons.Settings, R.string.tab_settings),
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

    DisposableEffect(Unit) {
        val listener: (XposedService?) -> Unit = { service -> xposedService = service }
        App.addServiceListener(listener)
        onDispose { App.removeServiceListener(listener) }
    }

    LaunchedEffect(pagerState.settledPage) {
        selectedTab = tabs[pagerState.settledPage]
    }

    val backgroundColor = appBackground(colorMode)

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            BottomNavigation(tabs, selectedTab) { index, tab ->
                selectedTab = tab
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
                    Tab.Home -> {
                        val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = stringResource(R.string.tab_home),
                                    largeTitle = stringResource(R.string.tab_home),
                                    scrollBehavior = scrollBehavior,
                                )
                            },
                            contentWindowInsets = WindowInsets.statusBars,
                        ) { pagePadding ->
                            HomePage(
                                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                                xposedService = xposedService,
                                clipboardMonitorMode = clipboardMonitorMode,
                                onClipboardMonitorModeChange = {
                                    clipboardMonitorMode = it
                                    settingsRepository.persistClipboardMonitorMode(it.value)
                                    ClipboardMonitorController.onModeChanged(context.applicationContext, it.value)
                                },
                                topContentPadding = pagePadding.calculateTopPadding() + 12.dp,
                                bottomContentPadding = pagePadding.calculateBottomPadding() + 16.dp,
                            )
                        }
                    }

                    Tab.Copy -> {
                        Scaffold(contentWindowInsets = WindowInsets.statusBars) { pagePadding ->
                            CloudRulesPage(
                                topContentPadding = pagePadding.calculateTopPadding() + 12.dp,
                                bottomContentPadding = 16.dp,
                            )
                        }
                    }

                    Tab.Rules -> {
                        val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
                        var showImportDialog by remember { mutableStateOf(false) }
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = stringResource(R.string.tab_rules),
                                    largeTitle = stringResource(R.string.tab_rules),
                                    scrollBehavior = scrollBehavior,
                                    actions = {
                                        IconButton(onClick = { showImportDialog = true }) {
                                            Icon(
                                                imageVector = MiuixIcons.Import,
                                                contentDescription = stringResource(R.string.action_import_rule),
                                            )
                                        }
                                    },
                                )
                            },
                            contentWindowInsets = WindowInsets.statusBars,
                        ) { pagePadding ->
                            RulesPage(
                                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                                showImportDialog = showImportDialog,
                                onDismissImportDialog = { showImportDialog = false },
                                topContentPadding = pagePadding.calculateTopPadding() + 12.dp,
                                bottomContentPadding = pagePadding.calculateBottomPadding() + 16.dp,
                            )
                        }
                    }

                    Tab.Settings -> {
                        val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = stringResource(R.string.tab_settings),
                                    largeTitle = stringResource(R.string.tab_settings),
                                    scrollBehavior = scrollBehavior,
                                )
                            },
                            contentWindowInsets = WindowInsets.statusBars,
                        ) { pagePadding ->
                            SettingsPage(
                                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                                onOpenTheme = { context.startActivity(Intent(context, ThemeSettingsActivity::class.java)) },
                                onOpenAppList = { context.startActivity(Intent(context, AppListActivity::class.java)) },
                                topContentPadding = pagePadding.calculateTopPadding() + 12.dp,
                                bottomContentPadding = pagePadding.calculateBottomPadding() + 16.dp,
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
