package io.github.hypercopy.ui

import android.widget.Toast
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.unit.dp
import io.github.hypercopy.App
import io.github.hypercopy.data.SettingsRepository
import io.github.hypercopy.ui.rules.RulesPage
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AppRecording
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class Tab(val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Home(MiuixIcons.Lock),
    Copy(MiuixIcons.Copy),
    Rules(MiuixIcons.AppRecording),
    Settings(MiuixIcons.Settings),
}

private enum class SettingsDestination {
    Main,
    Theme,
}

@Composable
fun AppScreen(
    colorMode: AppColorMode = AppColorMode.System,
    onColorModeChange: (AppColorMode) -> Unit = {},
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context.applicationContext) }
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

    val strings = zhStrings
    val backgroundColor = appBackground(colorMode)

    CompositionLocalProvider(LocalAppStrings provides strings) {
        Scaffold(
            bottomBar = {
                BottomNavigation(tabs, selectedTab, strings) { index, tab ->
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
                        Tab.Home -> HomePage(xposedService = xposedService, bottomContentPadding = 16.dp)
                        Tab.Copy -> EmptyPage(title = strings.copy)
                        Tab.Rules -> RulesPage(bottomContentPadding = 16.dp)
                        Tab.Settings -> AnimatedContent(
                            targetState = settingsDestination,
                            transitionSpec = {
                                val direction = if (targetState == SettingsDestination.Theme) {
                                    AnimatedContentTransitionScope.SlideDirection.Left
                                } else {
                                    AnimatedContentTransitionScope.SlideDirection.Right
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
                                    onCheckUpdate = {
                                        Toast.makeText(context, "暂未配置更新检查", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenTheme = { settingsDestination = SettingsDestination.Theme },
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
                            }
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
    strings: UiStrings,
    onTabClick: (Int, Tab) -> Unit,
) {
    NavigationBar(color = MiuixTheme.colorScheme.surface, showDivider = false) {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabClick(index, tab) },
                icon = tab.icon,
                label = tab.title(strings),
            )
        }
    }
}

private fun Tab.title(strings: UiStrings): String = when (this) {
    Tab.Home -> strings.home
    Tab.Copy -> strings.copy
    Tab.Rules -> strings.rules
    Tab.Settings -> strings.settings
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
