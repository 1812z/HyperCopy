package io.github.hypercopy.data

fun RuleConfig.resolveInputUrl(text: String): String {
    return extractMatchingWebUrl(text)?.let(::normalizeInputUrl) ?: normalizeInputUrl(text)
}

fun extractFirstInputUrl(text: String): String? {
    return WEB_URL_REGEX.findAll(text)
        .flatMap { webUrlCandidates(it.value) }
        .firstOrNull()
        ?.let(::normalizeInputUrl)
}

private fun RuleConfig.extractMatchingWebUrl(text: String): String? {
    return WEB_URL_REGEX.findAll(text)
        .flatMap { webUrlCandidates(it.value) }
        .firstOrNull { candidate -> matchesInput(candidate) || matchesInput(normalizeInputUrl(candidate)) }
}

private fun webUrlCandidates(raw: String): Sequence<String> = sequence {
    val trimmed = raw.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}', '\uFF0C', '\u3002', '\uFF1B', '\uFF1A', '\uFF01', '\uFF1F')
    val colonSegmentIndex = trimmed.indexOf("/:")
    if (colonSegmentIndex >= 0) yield(trimmed.substring(0, colonSegmentIndex + 1))
    yield(trimmed)
}

fun normalizeInputUrl(text: String): String {
    val value = text.trim()
    val uri = runCatching { android.net.Uri.parse(value) }.getOrNull()
    return if (uri?.scheme.isNullOrBlank()) "https://$value" else value
}

private val WEB_URL_REGEX = Regex("""(?:https?://)?[A-Za-z0-9.-]+\.[A-Za-z]{2,}(?:/[^\s]*)?""", RegexOption.IGNORE_CASE)
