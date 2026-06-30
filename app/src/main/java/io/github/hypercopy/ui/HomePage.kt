package io.github.hypercopy.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import io.github.hypercopy.Config
import io.github.hypercopy.R
import io.github.hypercopy.data.RuleRepository
import io.github.hypercopy.data.SettingsRepository
import io.github.libxposed.service.XposedService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomePage(xposedService: XposedService?, bottomContentPadding: Dp = 16.dp) {
    val context = LocalContext.current
    val systemInfo = remember { homeSystemInfo(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val ruleRepository = remember { RuleRepository(context) }
    val workMode = remember { settingsRepository.readClipboardMonitorMode() }
    val enabledRuleCount = remember { ruleRepository.readRules().count { it.enabled } }
    val isShizukuMode = workMode == Config.CLIPBOARD_MONITOR_MODE_SHIZUKU

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.tab_home),
                style = MiuixTheme.textStyles.title1,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            StatusCard(
                active = isShizukuMode || xposedService != null,
                workMode = workMode,
                enabledRuleCount = enabledRuleCount,
            )
        }
        item { InfoCard(systemInfo = systemInfo, xposedService = xposedService, showLsposedVersion = !isShizukuMode) }
    }
}

@Composable
private fun StatusCard(active: Boolean, workMode: String, enabledRuleCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val statusColor = if (active) Color(0xFF36D167) else Color(0xFFFF5A52)
        val statusBackground = if (active) Color(0xFFDFFAE4) else Color(0xFFFFE5E3)

        Card(
            modifier = Modifier.weight(1f).aspectRatio(1f),
            colors = CardDefaults.defaultColors(color = statusBackground),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxSize().offset(34.dp, 38.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Icon(
                        modifier = Modifier.size(136.dp),
                        imageVector = MiuixIcons.Copy,
                        contentDescription = null,
                        tint = statusColor.copy(alpha = 0.78f),
                    )
                }
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = stringResource(if (active) R.string.status_working else R.string.status_not_active),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF101010),
                    )
                    Text(
                        text = stringResource(
                            if (active) R.string.status_module_connected else R.string.status_module_disconnected,
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2F3A32).copy(alpha = 0.78f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f).aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                title = stringResource(R.string.home_work_mode),
                content = workModeLabel(workMode),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = stringResource(R.string.home_rule_count),
                content = stringResource(R.string.home_enabled_rule_count, enabledRuleCount),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(title: String, content: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = content,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun InfoCard(systemInfo: HomeSystemInfo, xposedService: XposedService?, showLsposedVersion: Boolean) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val unknown = stringResource(R.string.info_unknown)
            InfoText(title = stringResource(R.string.info_system_version), content = systemInfo.systemVersion)
            InfoText(title = stringResource(R.string.info_app_version), content = systemInfo.appVersion)
            InfoText(title = stringResource(R.string.info_android_version), content = systemInfo.androidVersion)
            if (showLsposedVersion) {
                InfoText(title = stringResource(R.string.info_lsposed_version), content = lsposedVersion(xposedService).ifBlank { unknown })
            }
            InfoText(title = stringResource(R.string.info_build_date), content = systemInfo.buildDate.ifBlank { unknown })
            InfoText(title = stringResource(R.string.info_device_model), content = systemInfo.deviceModel.ifBlank { unknown }, bottomPadding = 0.dp)
        }
    }
}

@Composable
private fun InfoText(title: String, content: String, bottomPadding: Dp = 24.dp) {
    Text(
        text = title,
        fontSize = MiuixTheme.textStyles.headline1.fontSize,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurface,
    )
    Text(
        text = content,
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding),
    )
}

private data class HomeSystemInfo(
    val systemVersion: String,
    val appVersion: String,
    val androidVersion: String,
    val buildDate: String,
    val deviceModel: String,
)

private fun homeSystemInfo(context: Context): HomeSystemInfo {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    val versionName = packageInfo.versionName ?: "unknown"
    return HomeSystemInfo(
        systemVersion = Build.DISPLAY,
        appVersion = "$versionName ($versionCode)",
        androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        buildDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(packageInfo.lastUpdateTime)),
        deviceModel = listOf(Build.MANUFACTURER, Build.MODEL).joinToString(" ").trim(),
    )
}

private fun lsposedVersion(service: XposedService?): String {
    if (service == null) return ""
    return runCatching {
        "${service.frameworkName} ${service.frameworkVersion} (${service.frameworkVersionCode}), API ${service.apiVersion}"
    }.getOrDefault("")
}

@Composable
private fun workModeLabel(value: String): String = stringResource(
    if (value == Config.CLIPBOARD_MONITOR_MODE_SHIZUKU) {
        R.string.clipboard_monitor_mode_shizuku
    } else {
        R.string.clipboard_monitor_mode_lsposed
    },
)
