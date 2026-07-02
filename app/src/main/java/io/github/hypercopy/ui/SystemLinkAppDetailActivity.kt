package io.github.hypercopy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.hypercopy.HyperLog
import io.github.hypercopy.R
import io.github.hypercopy.data.SettingsRepository
import io.github.hypercopy.data.SystemLinkApp
import io.github.hypercopy.data.SystemLinkDomain
import io.github.hypercopy.data.SystemLinkRepository
import io.github.hypercopy.ui.rules.PackageIcon
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.concurrent.thread

class SystemLinkAppDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_APP_LABEL = "app_label"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val userId = intent.getIntExtra(EXTRA_USER_ID, 0)
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty()

        if (packageName.isBlank()) {
            finish()
            return
        }

        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val systemLinkRepository = remember { SystemLinkRepository(applicationContext) }
            val colorMode = remember { appColorModeFromValue(settingsRepository.readColorMode()) }
            var systemLinkApp by remember { mutableStateOf<SystemLinkApp?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            fun loadApp(showLoading: Boolean = false) {
                if (showLoading) isLoading = true
                thread(name = "HyperCopySystemLinkDetail") {
                    val apps = runCatching { systemLinkRepository.readApps(userId) }
                        .getOrElse { throwable ->
                            HyperLog.d("HyperCopy", "load system links failed", throwable)
                            emptyList()
                        }
                    val app = apps.firstOrNull { it.packageName == packageName }
                    runOnUiThread {
                        systemLinkApp = app
                        isLoading = false
                    }
                }
            }

            LaunchedEffect(packageName) {
                loadApp(showLoading = true)
            }

            val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

            MiuixTheme(controller = ThemeController(colorSchemeModeOf(colorMode))) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = appLabel.ifBlank { packageName },
                            largeTitle = appLabel.ifBlank { packageName },
                            scrollBehavior = scrollBehavior,
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = MiuixIcons.Back, contentDescription = stringResource(R.string.action_back))
                                }
                            },
                        )
                    },
                ) { paddingValues ->
                    if (isLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.rule_system_link_title),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    } else {
                        val app = systemLinkApp
                        if (app == null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(R.string.rule_system_empty_description),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                                    .padding(paddingValues),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                item {
                                    AppInfoCard(app = app)
                                }
                                item {
                                    AppLinkAllowedCard(
                                        app = app,
                                        onEnabledChange = { enabled ->
                                            thread(name = "HyperCopySystemLinkAppToggle") {
                                                runCatching { systemLinkRepository.setLinkHandlingAllowed(userId, app.packageName, enabled) }
                                                    .onFailure { HyperLog.d("HyperCopy", "toggle app system link failed", it) }
                                                loadApp()
                                            }
                                        },
                                    )
                                }
                                if (app.domains.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.rule_system_app_link_allowed_summary),
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            modifier = Modifier.padding(start = 4.dp),
                                        )
                                    }
                                    items(app.domains, key = { it.host }) { domain ->
                                        DomainCard(
                                            domain = domain,
                                            onEnabledChange = { enabled ->
                                                thread(name = "HyperCopySystemLinkDomainToggle") {
                                                    runCatching { systemLinkRepository.setDomainEnabled(userId, app.packageName, domain.host, enabled) }
                                                        .onFailure { HyperLog.d("HyperCopy", "toggle domain link failed", it) }
                                                    loadApp()
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppInfoCard(app: SystemLinkApp) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PackageIcon(packageName = app.packageName, modifier = Modifier.padding(end = 12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = app.label, style = MiuixTheme.textStyles.headline1)
                Text(
                    text = app.packageName,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AppLinkAllowedCard(
    app: SystemLinkApp,
    onEnabledChange: (Boolean) -> Unit,
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.rule_system_app_link_allowed), style = MiuixTheme.textStyles.headline1)
                Text(
                    text = stringResource(R.string.rule_system_app_link_allowed_summary),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Switch(
                checked = app.linkHandlingAllowed,
                onCheckedChange = { onEnabledChange(!app.linkHandlingAllowed) },
            )
        }
    }
}

@Composable
private fun DomainCard(
    domain: SystemLinkDomain,
    onEnabledChange: (Boolean) -> Unit,
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = domain.host, style = MiuixTheme.textStyles.headline1)
                Text(
                    text = domain.state,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Switch(
                checked = domain.enabled,
                onCheckedChange = { onEnabledChange(!domain.enabled) },
            )
        }
    }
}
