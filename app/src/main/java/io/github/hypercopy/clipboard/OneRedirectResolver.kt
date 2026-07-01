package io.github.hypercopy.clipboard

import io.github.hypercopy.HyperLog
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object OneRedirectResolver {
    private const val TAG = "HyperCopy"
    private const val TIMEOUT_MILLIS = 2_000

    fun resolve(url: String): String {
        val normalized = normalizeUrl(url)
        return runCatching {
            val headResult = request(normalized, "HEAD")
            if (headResult != normalized) headResult else request(normalized, "GET")
        }
            .getOrElse { error ->
                HyperLog.d(TAG, "one redirect resolve failed: ${error.message}")
                normalized
            }
    }

    private fun request(url: String, method: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            instanceFollowRedirects = false
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            setRequestProperty("User-Agent", "HyperCopy")
        }
        return try {
            val code = connection.responseCode
            val location = connection.getHeaderField("Location")
            if (code in 300..399 && !location.isNullOrBlank()) resolveLocation(url, location) else url
        } finally {
            connection.disconnect()
        }
    }

    private fun resolveLocation(baseUrl: String, location: String): String {
        return runCatching { URI(baseUrl).resolve(location).toString() }.getOrDefault(location)
    }

    private fun normalizeUrl(text: String): String {
        val value = text.trim()
        return if (value.startsWith("http://", true) || value.startsWith("https://", true)) value else "https://$value"
    }
}
