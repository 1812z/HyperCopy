package io.github.hypercopy

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object AppIconCache {
    private val icons = ConcurrentHashMap<String, Bitmap>()
    private val missing = ConcurrentHashMap.newKeySet<String>()

    fun get(packageName: String): Bitmap? = icons[packageName]

    fun hasResult(packageName: String): Boolean = packageName.isBlank() || icons.containsKey(packageName) || packageName in missing

    fun loadNow(context: Context, packageName: String): Bitmap? {
        if (packageName.isBlank()) return null
        icons[packageName]?.let { return it }
        if (packageName in missing) return null

        return runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(width = 96, height = 96)
        }.onSuccess { icon ->
            icons[packageName] = icon
        }.onFailure {
            missing += packageName
        }.getOrNull()
    }

    suspend fun load(context: Context, packageName: String): Bitmap? = withContext(Dispatchers.IO) {
        loadNow(context, packageName)
    }
}
