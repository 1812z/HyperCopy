package io.github.hypercopy.ui.activities

import android.os.Bundle
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import io.github.hypercopy.data.SettingsRepository
import io.github.hypercopy.ui.framework.AppLanguage
import io.github.hypercopy.ui.framework.appLanguageFromValue
import io.github.hypercopy.ui.framework.appColorModeFromValue
import io.github.hypercopy.ui.framework.colorSchemeModeOf
import io.github.hypercopy.ui.pages.systemlinkdetail.SystemLinkAppDetailPage
import java.util.Locale
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class SystemLinkAppDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        if (packageName.isBlank()) {
            finish()
            return
        }
        val userId = intent.getIntExtra(EXTRA_USER_ID, 0)
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty()
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val colorMode = remember { appColorModeFromValue(settingsRepository.readColorMode()) }
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
                LocalActivityResultRegistryOwner provides this@SystemLinkAppDetailActivity,
            ) {
                MiuixTheme(controller = ThemeController(colorSchemeModeOf(colorMode))) {
                    SystemLinkAppDetailPage(
                        packageName = packageName,
                        userId = userId,
                        appLabel = appLabel,
                        onBack = ::finish,
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_APP_LABEL = "app_label"
    }
}
