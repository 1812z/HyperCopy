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
import io.github.hypercopy.data.ruleCategoryFromValue
import io.github.hypercopy.ui.framework.AppLanguage
import io.github.hypercopy.ui.framework.appLanguageFromValue
import io.github.hypercopy.ui.framework.appColorModeFromValue
import io.github.hypercopy.ui.framework.colorSchemeModeOf
import io.github.hypercopy.ui.pages.rules.RuleEditorPage
import java.util.Locale
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class RuleEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                LocalActivityResultRegistryOwner provides this@RuleEditorActivity,
            ) {
                MiuixTheme(controller = ThemeController(colorSchemeModeOf(colorMode))) {
                    RuleEditorPage(
                        ruleId = intent.getStringExtra(EXTRA_RULE_ID).orEmpty(),
                        initialCategory = ruleCategoryFromValue(intent.getStringExtra(EXTRA_CATEGORY).orEmpty()),
                        initialSourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL).orEmpty(),
                        initialTargetUrl = intent.getStringExtra(EXTRA_TARGET_URL).orEmpty(),
                        onBack = ::finish,
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_SOURCE_URL = "source_url"
        const val EXTRA_TARGET_URL = "target_url"
        const val EXTRA_RULE_ID = "rule_id"
        const val EXTRA_CATEGORY = "category"
    }
}
