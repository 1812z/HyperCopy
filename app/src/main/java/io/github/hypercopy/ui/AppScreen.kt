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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.hypercopy.App
import io.github.hypercopy.Config
import io.github.hypercopy.R
import io.github.hypercopy.clipboard.monitor.ClipboardMonitorController
import io.github.hypercopy.data.SettingsRepository
import io.github.hypercopy.data.UpdateCheckResult
import io.github.hypercopy.data.UpdateRepository
import io.github.hypercopy.ui.rules.CloudRulesPage
import io.github.hypercopy.ui.rules.RulesPage
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AppRecording
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Carrier
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayDialog
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
    val uriHandler = LocalUriHandler.current
    val settingsRepository = remember { SettingsRepository(context.applicationContext) }
    val updateRepository = remember { UpdateRepository(context.applicationContext) }
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
    var detectClonedApp by remember { mutableStateOf(settingsRepository.readDetectClonedApp()) }
    var appLanguage by remember { mutableStateOf(appLanguageFromValue(settingsRepository.readAppLanguage())) }
    var clipboardMonitorMode by remember {
        mutableStateOf(clipboardMonitorModeFromValue(settingsRepository.readClipboardMonitorMode()))
    }
    var jumpNotificationMode by remember {
        mutableStateOf(jumpNotificationModeFromValue(settingsRepository.readJumpNotificationMode()))
    }
    var updateDialog by remember { mutableStateOf<UpdateDialogState?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener: (XposedService?) -> Unit = { service -> xposedService = service }
        App.addServiceListener(listener)
        onDispose { App.removeServiceListener(listener) }
    }

    LaunchedEffect(pagerState.settledPage) {
        selectedTab = tabs[pagerState.settledPage]
    }

    LaunchedEffect(Unit) {
        if (autoCheckUpdate) {
            checkingUpdate = true
            val result = withContext(Dispatchers.IO) { updateRepository.checkLatestRelease() }
            checkingUpdate = false
            if (result is UpdateCheckResult.HasUpdate) {
                updateDialog = UpdateDialogState(
                    title = context.getString(R.string.update_new_version),
                    message = context.getString(
                        R.string.update_current_latest_version,
                        result.currentVersion,
                        result.release.version,
                    ),
                    url = result.release.url,
                    showOpenButton = true,
                )
            }
        }
    }

    fun checkUpdate(showNoUpdate: Boolean) {
        if (checkingUpdate) return
        checkingUpdate = true
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) { updateRepository.checkLatestRelease() }
            checkingUpdate = false
            when (result) {
                is UpdateCheckResult.HasUpdate -> updateDialog = UpdateDialogState(
                    title = context.getString(R.string.update_new_version),
                    message = context.getString(
                        R.string.update_current_latest_version,
                        result.currentVersion,
                        result.release.version,
                    ),
                    url = result.release.url,
                    showOpenButton = true,
                )

                is UpdateCheckResult.NoUpdate -> if (showNoUpdate) {
                    updateDialog = UpdateDialogState(
                        title = context.getString(R.string.update_latest_version),
                        message = context.getString(R.string.update_current_version, result.currentVersion),
                    )
                }

                is UpdateCheckResult.Failed -> updateDialog = UpdateDialogState(
                    title = context.getString(R.string.update_check_failed),
                    message = localizedUpdateFailure(context.getString(R.string.update_check_failed), result.message),
                )
            }
        }
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
                        val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
                        var showCloudMenu by remember { mutableStateOf(false) }
                        var showCloudSourcePopup by remember { mutableStateOf(false) }
                        var showInstalledOnly by remember { mutableStateOf(false) }
                        var cloudSource by remember { mutableStateOf(settingsRepository.readCloudSource()) }
                        var refreshTrigger by remember { mutableIntStateOf(0) }
                        var downloadInstalledTrigger by remember { mutableIntStateOf(0) }

                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = stringResource(R.string.tab_cloud_rules),
                                    largeTitle = stringResource(R.string.tab_cloud_rules),
                                    scrollBehavior = scrollBehavior,
                                    actions = {
                                        IconButton(onClick = { refreshTrigger++ }) {
                                            Icon(
                                                imageVector = MiuixIcons.Refresh,
                                                contentDescription = stringResource(R.string.action_refresh),
                                            )
                                        }
                                        Box {
                                            IconButton(onClick = { showCloudMenu = true }) {
                                                Icon(
                                                    imageVector = MiuixIcons.ListView,
                                                    contentDescription = stringResource(R.string.cloud_menu_options),
                                                )
                                            }
                                            OverlayCascadingListPopup(
                                                show = showCloudMenu,
                                                entries = listOf(
                                                    DropdownEntry(
                                                        items = listOf(
                                                            DropdownItem(
                                                                text = stringResource(R.string.cloud_menu_show_installed_only),
                                                                selected = showInstalledOnly,
                                                                onClick = {
                                                                    showCloudMenu = false
                                                                    showInstalledOnly = !showInstalledOnly
                                                                },
                                                            ),
                                                            DropdownItem(
                                                                text = stringResource(R.string.cloud_menu_download_installed),
                                                                onClick = {
                                                                    showCloudMenu = false
                                                                    downloadInstalledTrigger++
                                                                },
                                                            ),
                                                            DropdownItem(
                                                                text = stringResource(R.string.cloud_menu_switch_source),
                                                                onClick = {
                                                                    showCloudMenu = false
                                                                    showCloudSourcePopup = true
                                                                },
                                                            ),
                                                        ),
                                                    ),
                                                ),
                                                onDismissRequest = { showCloudMenu = false },
                                            )
                                            OverlayCascadingListPopup(
                                                show = showCloudSourcePopup,
                                                entries = listOf(
                                                    DropdownEntry(
                                                        items = listOf(
                                                            DropdownItem(
                                                                text = stringResource(R.string.cloud_source_accelerated),
                                                                selected = cloudSource == Config.CLOUD_SOURCE_ACCELERATED,
                                                                onClick = {
                                                                    showCloudSourcePopup = false
                                                                    settingsRepository.persistCloudSource(Config.CLOUD_SOURCE_ACCELERATED)
                                                                    cloudSource = Config.CLOUD_SOURCE_ACCELERATED
                                                                },
                                                            ),
                                                            DropdownItem(
                                                                text = stringResource(R.string.cloud_source_github),
                                                                selected = cloudSource == Config.CLOUD_SOURCE_GITHUB,
                                                                onClick = {
                                                                    showCloudSourcePopup = false
                                                                    settingsRepository.persistCloudSource(Config.CLOUD_SOURCE_GITHUB)
                                                                    cloudSource = Config.CLOUD_SOURCE_GITHUB
                                                                },
                                                            ),
                                                        ),
                                                    ),
                                                ),
                                                onDismissRequest = { showCloudSourcePopup = false },
                                            )
                                        }
                                    },
                                )
                            },
                            contentWindowInsets = WindowInsets.statusBars,
                        ) { pagePadding ->
                            CloudRulesPage(
                                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                                topContentPadding = pagePadding.calculateTopPadding() + 12.dp,
                                bottomContentPadding = 16.dp,
                                showInstalledOnly = showInstalledOnly,
                                cloudSource = cloudSource,
                                refreshTrigger = refreshTrigger,
                                downloadInstalledTrigger = downloadInstalledTrigger,
                            )
                        }
                    }

                    Tab.Rules -> {
                        val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
                        var showImportDialog by remember { mutableStateOf(false) }
                        var showRulesMenu by remember { mutableStateOf(false) }
                        var ruleSortMode by remember { mutableStateOf(false) }
                        var ruleEditMode by remember { mutableStateOf(false) }
                        var ruleActionsAvailable by remember { mutableStateOf(false) }
                        var systemLinkUserId by remember { mutableStateOf(settingsRepository.readSystemLinkUserId()) }
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = stringResource(R.string.tab_rules),
                                    largeTitle = stringResource(R.string.tab_rules),
                                    scrollBehavior = scrollBehavior,
                                    actions = {
                                        if (ruleActionsAvailable) {
                                            IconButton(onClick = {
                                                ruleEditMode = false
                                                ruleSortMode = true
                                            }) {
                                                Icon(
                                                    imageVector = MiuixIcons.Filter,
                                                    contentDescription = stringResource(R.string.action_sort_rule),
                                                )
                                            }
                                            IconButton(onClick = {
                                                ruleSortMode = false
                                                ruleEditMode = true
                                            }) {
                                                Icon(
                                                    imageVector = MiuixIcons.Edit,
                                                    contentDescription = stringResource(R.string.action_edit_rules),
                                                )
                                            }
                                        }
                                        if (!ruleActionsAvailable) {
                                            Box {
                                                IconButton(onClick = { showRulesMenu = true }) {
                                                    Icon(
                                                        imageVector = MiuixIcons.ListView,
                                                        contentDescription = stringResource(R.string.rule_system_user_menu),
                                                    )
                                                }
                                                OverlayCascadingListPopup(
                                                    show = showRulesMenu,
                                                    entries = listOf(
                                                        DropdownEntry(
                                                            items = listOf(
                                                                DropdownItem(
                                                                    text = stringResource(R.string.rule_system_user_0),
                                                                    selected = systemLinkUserId == 0,
                                                                    onClick = {
                                                                        showRulesMenu = false
                                                                        systemLinkUserId = 0
                                                                        settingsRepository.persistSystemLinkUserId(0)
                                                                    },
                                                                ),
                                                                DropdownItem(
                                                                    text = stringResource(R.string.rule_system_user_999),
                                                                    selected = systemLinkUserId == 999,
                                                                    onClick = {
                                                                        showRulesMenu = false
                                                                        systemLinkUserId = 999
                                                                        settingsRepository.persistSystemLinkUserId(999)
                                                                    },
                                                                ),
                                                            ),
                                                        ),
                                                    ),
                                                    onDismissRequest = { showRulesMenu = false },
                                                )
                                            }
                                        }
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
                                sortMode = ruleSortMode,
                                onSortModeChange = { ruleSortMode = it },
                                editMode = ruleEditMode,
                                onEditModeChange = { ruleEditMode = it },
                                onRuleActionsAvailableChange = { ruleActionsAvailable = it },
                                topContentPadding = pagePadding.calculateTopPadding() + 12.dp,
                                bottomContentPadding = pagePadding.calculateBottomPadding() + 16.dp,
                                systemLinkUserId = systemLinkUserId,
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
                                detectClonedApp = detectClonedApp,
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
                                onDetectClonedAppChange = {
                                    detectClonedApp = it
                                    settingsRepository.persistDetectClonedApp(it)
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
                                onCheckUpdate = { checkUpdate(showNoUpdate = true) },
                                onOpenTheme = { context.startActivity(Intent(context, ThemeSettingsActivity::class.java)) },
                                onOpenAppList = { context.startActivity(Intent(context, AppListActivity::class.java)) },
                                topContentPadding = pagePadding.calculateTopPadding() + 12.dp,
                                bottomContentPadding = pagePadding.calculateBottomPadding() + 16.dp,
                            )
                        }
                    }
                }
            }

            updateDialog?.let { dialog ->
                OverlayDialog(
                    title = dialog.title,
                    summary = dialog.message,
                    show = true,
                    onDismissRequest = { updateDialog = null },
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            text = stringResource(R.string.action_close),
                            onClick = { updateDialog = null },
                            modifier = Modifier.weight(1f),
                        )

                        if (dialog.showOpenButton && dialog.url != null) {
                            Spacer(Modifier.width(20.dp))
                            TextButton(
                                text = stringResource(R.string.action_open),
                                onClick = {
                                    updateDialog = null
                                    uriHandler.openUri(dialog.url)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun localizedUpdateFailure(defaultMessage: String, message: String): String = when (message) {
    "未找到 Release 版本号" -> "未找到 Release 版本号"
    "检查更新失败" -> defaultMessage
    else -> message
}

private data class UpdateDialogState(
    val title: String,
    val message: String,
    val url: String? = null,
    val showOpenButton: Boolean = false,
)

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
