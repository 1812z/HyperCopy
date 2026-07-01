package io.github.hypercopy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import io.github.hypercopy.data.SettingsRepository
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            var colorMode by remember { mutableStateOf(appColorModeFromValue(settingsRepository.readColorMode())) }
            val lifecycleOwner = LocalLifecycleOwner.current
            val controller = remember(colorMode) { ThemeController(colorSchemeModeOf(colorMode)) }
            val navigationEventDispatcherOwner = remember {
                object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = NavigationEventDispatcher()
                }
            }

            DisposableEffect(lifecycleOwner, settingsRepository) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        colorMode = appColorModeFromValue(settingsRepository.readColorMode())
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
