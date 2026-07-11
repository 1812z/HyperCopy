package io.github.hypercopy.ui.activities

import android.os.Bundle
import android.content.ContextWrapper
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import io.github.hypercopy.data.settings.SettingsRepository
import io.github.hypercopy.ui.framework.AppLanguage
import io.github.hypercopy.ui.framework.appLanguageFromValue
import io.github.hypercopy.ui.framework.appColorModeFromValue
import io.github.hypercopy.ui.framework.colorSchemeModeOf
import io.github.hypercopy.ui.pages.themesettings.ThemeSettingsPage
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class ThemeSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            var colorMode by remember { mutableStateOf(appColorModeFromValue(settingsRepository.readColorMode())) }
            val appLanguage = appLanguageFromValue(settingsRepository.readAppLanguage())
            val activityContext = LocalContext.current
            val configuration = LocalConfiguration.current
            val localizedContext = remember(appLanguage, activityContext, configuration) {
                if (appLanguage == AppLanguage.System) {
                    activityContext
                } else {
                    val config = android.content.res.Configuration(configuration)
                    config.setLocale(Locale.forLanguageTag(appLanguage.value))
                    val localeContext = activityContext.createConfigurationContext(config)
                    object : ContextWrapper(activityContext) {
                        override fun getAssets() = localeContext.assets
                        override fun getResources() = localeContext.resources
                    }
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@ThemeSettingsActivity,
            ) {
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
