package io.github.hypercopy.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.hypercopy.Config
import io.github.hypercopy.R
import io.github.hypercopy.clipboard.monitor.RootPermission
import io.github.hypercopy.clipboard.monitor.ShizukuPermission
import io.github.hypercopy.data.RuleRepository
import io.github.libxposed.service.XposedService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    xposedService: XposedService?,
    clipboardMonitorMode: ClipboardMonitorMode,
    onClipboardMonitorModeChange: (ClipboardMonitorMode) -> Unit,
    topContentPadding: Dp = 12.dp,
    bottomContentPadding: Dp = 16.dp,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val systemInfo = remember { homeSystemInfo(context) }
    val ruleRepository = remember { RuleRepository(context) }
    val enabledRuleCount = remember { ruleRepository.readRules().count { it.enabled } }
    val workMode = clipboardMonitorMode.value
    val isShizukuMode = clipboardMonitorMode == ClipboardMonitorMode.Shizuku
    val monitorModeOptions = clipboardMonitorModeOptions()
    var rootGranted by remember { mutableStateOf(false) }
    var shizukuGranted by remember { mutableStateOf(ShizukuPermission.isGranted()) }
    var batteryUnrestricted by remember { mutableStateOf(isBatteryUnrestricted(context)) }

    fun refreshPermissionStatus(requestCurrentModePermission: Boolean) {
        batteryUnrestricted = isBatteryUnrestricted(context)
        if (isShizukuMode) {
            ShizukuPermission.waitForAvailable { available ->
                if (!available) {
                    shizukuGranted = false
                } else if (requestCurrentModePermission) {
                    ShizukuPermission.requestIfNeeded { granted -> shizukuGranted = granted }
                } else {
                    shizukuGranted = ShizukuPermission.isGranted()
                }
            }
        } else {
            coroutineScope.launch {
                rootGranted = withContext(Dispatchers.IO) {
                    if (requestCurrentModePermission) RootPermission.request() else RootPermission.isGranted()
                }
            }
        }
    }

    LaunchedEffect(clipboardMonitorMode) {
        refreshPermissionStatus(requestCurrentModePermission = true)
    }

    DisposableEffect(lifecycleOwner, clipboardMonitorMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissionStatus(requestCurrentModePermission = false)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionGranted = if (isShizukuMode) shizukuGranted else rootGranted
    val active = if (isShizukuMode) shizukuGranted else xposedService != null && rootGranted

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, top = topContentPadding, end = 12.dp, bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StatusCard(
                active = active,
                batteryUnrestricted = batteryUnrestricted,
                workMode = workMode,
                enabledRuleCount = enabledRuleCount,
            )
        }
        item {
            MonitorModeCard(
                options = monitorModeOptions,
                selectedMode = clipboardMonitorMode,
                permissionGranted = permissionGranted,
                batteryUnrestricted = batteryUnrestricted,
                onModeChange = onClipboardMonitorModeChange,
                onRequestPermission = { refreshPermissionStatus(requestCurrentModePermission = true) },
                onOpenBatterySettings = { openBatterySettings(context) },
            )
        }
        item { InfoCard(systemInfo = systemInfo, xposedService = xposedService, showLsposedVersion = !isShizukuMode) }
    }
}

@Composable
private fun StatusCard(active: Boolean, batteryUnrestricted: Boolean, workMode: String, enabledRuleCount: Int) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MainStatusCard(
                    active = active,
                    batteryUnrestricted = batteryUnrestricted,
                    modifier = Modifier.weight(1f).height(112.dp),
                )
                StatCard(
                    title = stringResource(R.string.home_work_mode),
                    content = workModeLabel(workMode),
                    modifier = Modifier.weight(1f).height(112.dp),
                )
                StatCard(
                    title = stringResource(R.string.home_rule_count),
                    content = stringResource(R.string.home_enabled_rule_count, enabledRuleCount),
                    modifier = Modifier.weight(1f).height(112.dp),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MainStatusCard(
                    active = active,
                    batteryUnrestricted = batteryUnrestricted,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                )
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
    }
}

@Composable
private fun MainStatusCard(active: Boolean, batteryUnrestricted: Boolean, modifier: Modifier = Modifier) {
    val warning = active && !batteryUnrestricted
    val statusColor = when {
        warning -> Color(0xFFFF9F0A)
        active -> Color(0xFF36D167)
        else -> Color(0xFFFF5A52)
    }
    val statusBackground = when {
        warning -> Color(0xFFFFF1D6)
        active -> Color(0xFFDFFAE4)
        else -> Color(0xFFFFE5E3)
    }

    Card(
        modifier = modifier,
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
                        when {
                            warning -> R.string.status_battery_abnormal
                            active -> R.string.status_module_connected
                            else -> R.string.status_module_disconnected
                        },
                    ),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (warning) Color(0xFFE07000) else Color(0xFF2F3A32).copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun MonitorModeCard(
    options: List<ClipboardMonitorModeOption>,
    selectedMode: ClipboardMonitorMode,
    permissionGranted: Boolean,
    batteryUnrestricted: Boolean,
    onModeChange: (ClipboardMonitorMode) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            OverlayDropdownPreference(
                title = stringResource(R.string.clipboard_monitor_mode),
                summary = stringResource(R.string.clipboard_monitor_mode_summary),
                items = options.map { it.label },
                selectedIndex = options.indexOfFirst { it.value == selectedMode }.coerceAtLeast(0),
                insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                onSelectedIndexChange = { onModeChange(options[it].value) },
            )
            StatusActionRow(
                title = stringResource(if (selectedMode == ClipboardMonitorMode.Shizuku) R.string.permission_shizuku_status else R.string.permission_root_status),
                content = stringResource(if (permissionGranted) R.string.permission_granted else R.string.permission_missing),
                showAction = !permissionGranted,
                actionContentDescription = stringResource(R.string.action_request_permission),
                onActionClick = onRequestPermission,
            )
            if (!batteryUnrestricted) {
                StatusActionRow(
                    title = stringResource(R.string.battery_status),
                    content = stringResource(R.string.status_battery_abnormal),
                    showAction = true,
                    actionContentDescription = stringResource(R.string.action_battery_settings),
                    onActionClick = onOpenBatterySettings,
                )
            }
        }
    }
}

@Composable
private fun StatusActionRow(
    title: String,
    content: String,
    showAction: Boolean,
    actionContentDescription: String,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            InfoText(title = title, content = content, bottomPadding = 0.dp)
        }
        if (showAction) {
            IconButton(
                onClick = onActionClick,
                minWidth = 32.dp,
                minHeight = 32.dp,
                cornerRadius = 16.dp,
                backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.padding(start = 10.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.ChevronForward,
                    contentDescription = actionContentDescription,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(16.dp),
                )
            }
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

private fun isBatteryUnrestricted(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatterySettings(context: Context) {
    val packageUri = Uri.parse("package:${context.packageName}")
    val intents = listOf(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )
    for (intent in intents) {
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }.onFailure { throwable ->
            if (throwable !is ActivityNotFoundException) return@onFailure
        }
    }
}

private data class ClipboardMonitorModeOption(val label: String, val value: ClipboardMonitorMode)

@Composable
private fun clipboardMonitorModeOptions() = listOf(
    ClipboardMonitorModeOption(stringResource(R.string.clipboard_monitor_mode_lsposed), ClipboardMonitorMode.LSPosed),
    ClipboardMonitorModeOption(stringResource(R.string.clipboard_monitor_mode_shizuku), ClipboardMonitorMode.Shizuku),
)

@Composable
private fun workModeLabel(value: String): String = stringResource(
    if (value == Config.CLIPBOARD_MONITOR_MODE_SHIZUKU) {
        R.string.clipboard_monitor_mode_shizuku
    } else {
        R.string.clipboard_monitor_mode_lsposed
    },
)
