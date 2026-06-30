package io.github.hypercopy.ui.rules

import android.content.Context
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

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
            Image(bitmap = it, contentDescription = null, modifier = Modifier.size(40.dp))
        }
    }
}

private object AppIconCache {
    private val icons = ConcurrentHashMap<String, ImageBitmap>()
    private val missing = ConcurrentHashMap.newKeySet<String>()

    fun get(packageName: String): ImageBitmap? = icons[packageName]

    fun hasResult(packageName: String): Boolean = packageName.isBlank() || icons.containsKey(packageName) || packageName in missing

    suspend fun load(context: Context, packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        icons[packageName]?.let { return@withContext it }
        if (packageName in missing) return@withContext null

        runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }.onSuccess { icon ->
            icons[packageName] = icon
        }.onFailure {
            missing += packageName
        }.getOrNull()
    }
}
