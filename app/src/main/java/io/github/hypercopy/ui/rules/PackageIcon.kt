package io.github.hypercopy.ui.rules

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.hypercopy.AppIconCache

@Composable
internal fun PackageIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    var icon by remember(packageName) { mutableStateOf(AppIconCache.get(packageName)) }

    LaunchedEffect(packageName) {
        if (packageName.isBlank()) return@LaunchedEffect
        AppIconCache.get(packageName)?.let {
            icon = it
            return@LaunchedEffect
        }
        if (AppIconCache.hasResult(packageName)) return@LaunchedEffect
        icon = AppIconCache.load(context, packageName)
    }

    Box(modifier = modifier.size(40.dp)) {
        icon?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp))
        }
    }
}
