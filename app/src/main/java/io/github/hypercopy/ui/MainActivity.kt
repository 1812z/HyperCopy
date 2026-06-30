package io.github.hypercopy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import io.github.hypercopy.data.SettingsRepository
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            var colorMode by remember { mutableStateOf(appColorModeFromValue(settingsRepository.readColorMode())) }
            val controller = remember(colorMode) { ThemeController(colorMode.toColorSchemeMode()) }
            val navigationEventDispatcherOwner = remember {
                object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = NavigationEventDispatcher()
                }
            }

            MiuixTheme(controller = controller) {
                CompositionLocalProvider(
                    LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
                ) {
                    AppScreen(
                        colorMode = colorMode,
                        onColorModeChange = { colorMode = it },
                    )
                }
            }
        }
    }
}

private fun AppColorMode.toColorSchemeMode(): ColorSchemeMode = when (this) {
    AppColorMode.System -> ColorSchemeMode.System
    AppColorMode.Light -> ColorSchemeMode.Light
    AppColorMode.Dark -> ColorSchemeMode.Dark
}
