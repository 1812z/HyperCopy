package io.github.hypercopy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsAction(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    SettingsRow(icon = icon, title = title, summary = summary, role = Role.Button, onClick = onClick)
}

@Composable
fun SettingsActionWithArrow(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    SettingsRow(
        icon = icon,
        title = title,
        summary = summary,
        role = Role.Button,
        onClick = onClick,
        trailing = {
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

@Composable
fun SwitchAction(icon: ImageVector, title: String, summary: String, checked: Boolean, onCheckedChange: () -> Unit) {
    SettingsRow(
        icon = icon,
        title = title,
        summary = summary,
        role = Role.Switch,
        onClick = onCheckedChange,
        trailing = { Switch(checked = checked, onCheckedChange = { onCheckedChange() }) },
    )
}

@Composable
fun SettingsIcon(imageVector: ImageVector) {
    Row(modifier = Modifier.width(32.dp)) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    summary: String,
    role: Role,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = role, onClick = onClick).padding(SettingsItemMargin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = if (trailing == null) {
                Modifier.padding(start = SettingsTextStartPadding)
            } else {
                Modifier.padding(start = SettingsTextStartPadding, end = 12.dp).weight(1f)
            },
        ) {
            Text(text = title, style = MiuixTheme.textStyles.headline1)
            Text(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        trailing?.invoke()
    }
}

private val SettingsItemMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
private val SettingsTextStartPadding = 16.dp
