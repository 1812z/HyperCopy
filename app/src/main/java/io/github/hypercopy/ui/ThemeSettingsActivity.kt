package io.github.hypercopy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.hypercopy.data.SettingsRepository
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class ThemeSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            var colorMode by remember { mutableStateOf(appColorModeFromValue(settingsRepository.readColorMode())) }

            CompositionLocalProvider(LocalActivityResultRegistryOwner provides this@ThemeSettingsActivity) {
                MiuixTheme(controller = ThemeController(colorSchemeModeOf(colorMode))) {
                    Scaffold { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            ThemeSettingsPage(
                                colorMode = colorMode,
                                onColorModeChange = {
                                    colorMode = it
                                    settingsRepository.persistColorMode(it.value)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
