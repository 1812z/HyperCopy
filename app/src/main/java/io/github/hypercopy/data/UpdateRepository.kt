package io.github.hypercopy.data

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val version: String,
    val url: String,
)

sealed interface UpdateCheckResult {
    data class HasUpdate(val release: ReleaseInfo, val currentVersion: String) : UpdateCheckResult
    data class NoUpdate(val currentVersion: String) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

class UpdateRepository(private val context: Context) {
    fun checkLatestRelease(): UpdateCheckResult {
        return try {
            val release = fetchLatestRelease() ?: return UpdateCheckResult.Failed("未找到 Release 版本号")
            val currentVersion = currentVersionName()

            if (compareVersions(release.version, currentVersion) > 0) {
                UpdateCheckResult.HasUpdate(release, currentVersion)
            } else {
                UpdateCheckResult.NoUpdate(currentVersion)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failed(e.message ?: "检查更新失败")
        }
    }

    private fun fetchLatestRelease(): ReleaseInfo? {
        fetchBody(RELEASES_API_URL)?.let { body ->
            val releases = JSONArray(body)
            for (index in 0 until releases.length()) {
                val release = releases.optJSONObject(index) ?: continue
                if (release.optBoolean("draft")) continue
                parseRelease(release)?.let { return it }
            }
        }

        fetchBody(LATEST_RELEASE_API_URL)?.let { body ->
            parseRelease(JSONObject(body))?.let { return it }
        }

        return null
    }

    private fun fetchBody(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "HyperCopy")
        }

        return try {
            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }.takeIf { it.isNotBlank() }
            } else {
                throw IllegalStateException("检查更新失败")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(release: JSONObject): ReleaseInfo? {
        val version = release.optString("tag_name").takeIf { it.isNotBlank() } ?: return null
        val url = release.optString("html_url").takeIf { it.isNotBlank() } ?: RELEASE_URL
        return ReleaseInfo(version, url)
    }

    private fun currentVersionName(): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName ?: "0"
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        return if (versionName == "0") versionCode.toString() else versionName
    }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = versionParts(left)
        val rightParts = versionParts(right)
        val max = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until max) {
            val l = leftParts.getOrElse(index) { 0 }
            val r = rightParts.getOrElse(index) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    private fun versionParts(version: String): List<Int> {
        return version
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .split('.', '-', '_')
            .mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
    }

    private companion object {
        const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/1812z/HyperCopy/releases/latest"
        const val RELEASES_API_URL = "https://api.github.com/repos/1812z/HyperCopy/releases"
        const val RELEASE_URL = "https://github.com/1812z/HyperCopy/releases"
    }
}
