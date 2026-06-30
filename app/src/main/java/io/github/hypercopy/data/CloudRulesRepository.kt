package io.github.hypercopy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 远端规则仓库。从 https://github.com/1812z/HyperCopy_Rules 拉取 [link] / [text] 两个目录下的
 * JSON 规则文件，文件名约定：规则名称_应用包名.json。
 */
class CloudRulesRepository {

    suspend fun listRules(folder: String): List<CloudRule> = withContext(Dispatchers.IO) {
        val endpoint = "$API_BASE/repos/$REPO/contents/$folder"
        val response = readJsonArray(endpoint)
        buildList {
            for (index in 0 until response.length()) {
                val entry = response.optJSONObject(index) ?: continue
                if (entry.optString("type") != "file") continue
                val fileName = entry.optString("name")
                if (!fileName.endsWith(".json", ignoreCase = true)) continue
                val parsed = parseRuleFileName(fileName) ?: continue
                add(
                    CloudRule(
                        name = parsed.name,
                        packageName = parsed.packageName,
                        fileName = fileName,
                        folder = folder,
                        downloadUrl = entry.optString("download_url"),
                        size = entry.optLong("size", 0L),
                    ),
                )
            }
        }
    }

    suspend fun downloadRule(cloudRule: CloudRule): RuleConfig = withContext(Dispatchers.IO) {
        if (cloudRule.downloadUrl.isBlank()) throw CloudRuleException(CloudRuleError.MissingDownloadUrl)
        val raw = readText(cloudRule.downloadUrl)
        val parsed = parseRuleContent(raw)
        // 以云端来源作为稳定 ID，重复下载为更新而非新增
        val stableId = "cloud_${cloudRule.folder}_${cloudRule.fileNameWithoutExt()}"
        val category = resolveCategory(cloudRule.folder, parsed.category)
        parsed.copy(id = stableId, name = cloudRule.name, category = category)
    }

    private fun parseRuleContent(text: String): RuleConfig {
        if (text.isBlank()) return RuleConfig(name = "", matchRegex = "", parameterRegex = "", target = RuleTarget(type = RuleTargetType.Url, template = ""))
        val trimmed = text.trim()
        return when {
            trimmed.startsWith("[") -> {
                val array = JSONArray(trimmed)
                val first = array.optJSONObject(0) ?: JSONObject()
                ruleConfigFromJson(first)
            }
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                val rulesArray = obj.optJSONArray("rules")
                if (rulesArray != null && rulesArray.length() > 0) {
                    ruleConfigFromJson(rulesArray.optJSONObject(0) ?: JSONObject())
                } else {
                    ruleConfigFromJson(obj)
                }
            }
            else -> ruleConfigFromJson(JSONObject())
        }
    }

    private fun resolveCategory(folder: String, parsed: RuleCategory): RuleCategory = when (folder) {
        FOLDER_LINK -> RuleCategory.Link
        FOLDER_TEXT -> when (parsed) {
            RuleCategory.Address, RuleCategory.Express -> parsed
            else -> RuleCategory.Address
        }
        else -> parsed
    }

    private fun readJsonArray(url: String): JSONArray {
        val (status, body) = httpGet(url)
        if (status == HttpURLConnection.HTTP_NOT_FOUND) return JSONArray()
        if (status != HttpURLConnection.HTTP_OK) throw CloudRuleException(CloudRuleError.LoadFailed)
        return JSONArray(body)
    }

    private fun readText(url: String): String {
        val (status, body) = httpGet(url)
        if (status != HttpURLConnection.HTTP_OK) throw CloudRuleException(CloudRuleError.DownloadFailed)
        return body
    }

    private fun httpGet(url: String): HttpResult {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", ACCEPT_HEADER)
                setRequestProperty("User-Agent", USER_AGENT)
                instanceFollowRedirects = true
            }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            HttpResult(code, body)
        } catch (e: Exception) {
            throw CloudRuleException(CloudRuleError.NetworkError, e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseRuleFileName(fileName: String): ParsedFileName? {
        if (!fileName.endsWith(".json", ignoreCase = true)) return null
        val base = fileName.removeSuffix(".json").removeSuffix(".JSON")
        val underscoreIndex = base.lastIndexOf('_')
        return if (underscoreIndex > 0 && underscoreIndex < base.length - 1) {
            ParsedFileName(base.substring(0, underscoreIndex), base.substring(underscoreIndex + 1))
        } else {
            ParsedFileName(base, "")
        }
    }

    private data class ParsedFileName(val name: String, val packageName: String)

    private data class HttpResult(val status: Int, val body: String)

    companion object {
        private const val API_BASE = "https://api.github.com"
        private const val REPO = "1812z/HyperCopy_Rules"
        private const val FOLDER_LINK = "link"
        private const val FOLDER_TEXT = "text"
        private const val TIMEOUT_MS = 15_000
        private const val ACCEPT_HEADER = "application/vnd.github+json"
        private const val USER_AGENT = "HyperCopy"
    }
}

data class CloudRule(
    val name: String,
    val packageName: String,
    val fileName: String,
    val folder: String,
    val downloadUrl: String,
    val size: Long,
) {
    fun fileNameWithoutExt(): String =
        if (fileName.endsWith(".json", ignoreCase = true)) fileName.removeSuffix(".json").removeSuffix(".JSON") else fileName

    val category: RuleCategory
        get() = if (folder == "text") RuleCategory.Address else RuleCategory.Link
}

enum class CloudRuleError {
    MissingDownloadUrl,
    LoadFailed,
    DownloadFailed,
    NetworkError,
}

class CloudRuleException(val error: CloudRuleError, cause: Throwable? = null) : Exception(error.name, cause)
