package io.github.hypercopy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.hypercopy.data.SettingsRepository
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.MiuixTheme

class AppListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val colorMode = remember { appColorModeFromValue(settingsRepository.readColorMode()) }
            var appListWorkMode by remember { mutableStateOf(settingsRepository.readAppListWorkMode()) }
            var ignoreJumpApp by remember { mutableStateOf(settingsRepository.readIgnoreJumpApp()) }
            var appListPackages by remember { mutableStateOf(settingsRepository.readAppListPackages()) }

            MiuixTheme(controller = ThemeController(colorSchemeModeOf(colorMode))) {
                Scaffold { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        AppListPage(
                            workMode = appListWorkMode,
                            ignoreJumpApp = ignoreJumpApp,
                            selectedPackages = appListPackages,
                            onWorkModeChange = {
                                appListWorkMode = it
                                settingsRepository.persistAppListWorkMode(it)
                            },
                            onIgnoreJumpAppChange = {
                                ignoreJumpApp = it
                                settingsRepository.persistIgnoreJumpApp(it)
                            },
                            onPackageCheckedChange = { packageName, checked ->
                                val updatedPackages = if (checked) {
                                    appListPackages + packageName
                                } else {
                                    appListPackages - packageName
                                }
                                appListPackages = updatedPackages
                                settingsRepository.persistAppListPackages(updatedPackages)
                            },
                            onBack = { finish() },
                        )
                    }
                }
            }
        }
    }
}
