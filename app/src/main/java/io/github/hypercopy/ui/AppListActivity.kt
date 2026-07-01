package io.github.hypercopy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.hypercopy.Config
import io.github.hypercopy.R
import io.github.hypercopy.data.SettingsRepository
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class AppListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val colorMode = remember { appColorModeFromValue(settingsRepository.readColorMode()) }
            var appListWorkMode by remember { mutableStateOf(settingsRepository.readAppListWorkMode()) }
            var ignoreJumpApp by remember { mutableStateOf(settingsRepository.readIgnoreJumpApp()) }
            var appListPackages by remember { mutableStateOf(settingsRepository.readAppListPackages()) }
            var showMenuPopup by remember { mutableStateOf(false) }
            val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
            val workModeOptions = listOf(
                stringResource(R.string.app_list_whitelist) to Config.APP_LIST_WORK_MODE_WHITELIST,
                stringResource(R.string.app_list_blacklist) to Config.APP_LIST_WORK_MODE_BLACKLIST,
            )
            val menuEntries = listOf(
                DropdownEntry(
                    items = listOf(
                        DropdownItem(
                            text = stringResource(R.string.app_list_work_mode),
                            children = workModeOptions.map { (label, value) ->
                                DropdownItem(
                                    text = label,
                                    selected = appListWorkMode == value,
                                    onClick = {
                                        showMenuPopup = false
                                        appListWorkMode = value
                                        settingsRepository.persistAppListWorkMode(value)
                                    },
                                )
                            },
                        ),
                        DropdownItem(
                            text = stringResource(R.string.app_list_ignore_jump),
                            selected = ignoreJumpApp,
                            onClick = {
                                showMenuPopup = false
                                ignoreJumpApp = !ignoreJumpApp
                                settingsRepository.persistIgnoreJumpApp(ignoreJumpApp)
                            },
                        ),
                    ),
                ),
            )

            MiuixTheme(controller = ThemeController(colorSchemeModeOf(colorMode))) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = stringResource(R.string.app_list),
                            largeTitle = stringResource(R.string.app_list),
                            scrollBehavior = scrollBehavior,
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = MiuixIcons.Back, contentDescription = stringResource(R.string.action_back))
                                }
                            },
                            actions = {
                                Box {
                                    IconButton(onClick = { showMenuPopup = true }) {
                                        Icon(
                                            imageVector = MiuixIcons.ListView,
                                            contentDescription = stringResource(R.string.app_list_menu_options),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    OverlayCascadingListPopup(
                                        show = showMenuPopup,
                                        entries = menuEntries,
                                        onDismissRequest = { showMenuPopup = false },
                                    )
                                }
                            },
                        )
                    },
                ) { paddingValues ->
                    AppListPage(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        selectedPackages = appListPackages,
                        onPackageCheckedChange = { packageName, checked ->
                            val updatedPackages = if (checked) {
                                appListPackages + packageName
                            } else {
                                appListPackages - packageName
                            }
                            appListPackages = updatedPackages
                            settingsRepository.persistAppListPackages(updatedPackages)
                        },
                        topContentPadding = paddingValues.calculateTopPadding() + 12.dp,
                        bottomContentPadding = paddingValues.calculateBottomPadding() + 16.dp,
                    )
                }
            }
        }
    }
}
