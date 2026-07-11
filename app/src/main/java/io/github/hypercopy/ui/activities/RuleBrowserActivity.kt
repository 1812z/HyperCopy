package io.github.hypercopy.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import io.github.hypercopy.data.SettingsRepository
import io.github.hypercopy.ui.framework.appColorModeFromValue
import io.github.hypercopy.ui.framework.colorSchemeModeOf
import io.github.hypercopy.ui.pages.rules.RuleBrowserPage
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class RuleBrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val colorMode = remember { appColorModeFromValue(settingsRepository.readColorMode()) }
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides this@RuleBrowserActivity) {
                MiuixTheme(controller = ThemeController(colorSchemeModeOf(colorMode))) {
                    RuleBrowserPage(onBack = ::finish)
                }
            }
        }
    }
}
