package io.github.hypercopy.data.systemlink

import android.content.Context
import io.github.hypercopy.HyperLog
import io.github.hypercopy.clipboard.privileged.IntentAmStartCommand
import io.github.hypercopy.clipboard.privileged.PrivilegedShell
import io.github.hypercopy.clipboard.privileged.ShellResult
import io.github.hypercopy.data.rules.normalizeInputUrl
import io.github.hypercopy.data.settings.SettingsRepository
import java.util.concurrent.ConcurrentHashMap

data class SystemLinkApp(
    val packageName: String,
    val label: String,
    val linkHandlingAllowed: Boolean,
    val domains: List<SystemLinkDomain>,
)

data class SystemLinkDomain(
    val host: String,
    val enabled: Boolean,
    val state: String,
)

class SystemLinkRepository(private val context: Context) {
    private val tag = "HyperCopy"
    private val settingsRepository = SettingsRepository(context.applicationContext)

    fun readApps(userId: Int): List<SystemLinkApp> {
        val output = runFirstSuccessful("pm get-app-links --user $userId")
        HyperLog.d(tag, "pm get-app-links output length=${output.length} head=${output.take(500)}")
        val apps = parseApps(output, userId)
        HyperLog.d(tag, "system link apps parsed user=$userId count=${apps.size}")
        return apps
            .sortedWith(compareBy<SystemLinkApp> { it.label }.thenBy { it.packageName })
    }

    fun readDomains(userId: Int, packageName: String): List<SystemLinkDomain> {
        val output = runFirstSuccessful(
            "pm get-app-links --user $userId ${IntentAmStartCommand.shellQuote(packageName)}",
        )
        return parseDomains(output)
    }

    fun setDomainEnabled(userId: Int, packageName: String, host: String, enabled: Boolean): Boolean {
        val value = if (enabled) "true" else "false"
        val result = runFirstSuccessfulResult(
            "pm set-app-links-user-selection --user $userId --package ${IntentAmStartCommand.shellQuote(packageName)} $value ${IntentAmStartCommand.shellQuote(host)}",
        )
        HyperLog.d(tag, "set domain link user=$userId package=$packageName host=$host enabled=$enabled code=${result.exitCode}")
        return result.exitCode == 0
    }

    fun setLinkHandlingAllowed(userId: Int, packageName: String, enabled: Boolean): Boolean {
        val result = runFirstSuccessfulResult(
            "pm set-app-links-allowed --user $userId --package ${IntentAmStartCommand.shellQuote(packageName)} $enabled",
        )
        HyperLog.d(tag, "set app link allowed user=$userId package=$packageName enabled=$enabled code=${result.exitCode}")
        return result.exitCode == 0
    }

    fun openLink(userId: Int, url: String): Boolean {
        val safeUrl = shellQuote(normalizeInputUrl(url))
        val output = runFirstSuccessful(
            "am start --user $userId -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d $safeUrl",
        )
        return output.contains("Starting", ignoreCase = true) || output.contains("Warning: Activity not started", ignoreCase = true)
    }

    fun isPackageInstalledForUser(userId: Int, packageName: String): Boolean {
        val cacheKey = "$userId:$packageName"
        val now = System.currentTimeMillis()
        packageInstallCache[cacheKey]?.takeIf { now - it.checkedAt < PACKAGE_INSTALL_CACHE_MILLIS }?.let { return it.installed }
        val result = runFirstSuccessfulResult("pm path --user $userId ${IntentAmStartCommand.shellQuote(packageName)}")
        val installed = result.exitCode == 0 && result.output.contains("package:")
        packageInstallCache[cacheKey] = PackageInstallCacheEntry(installed, now)
        HyperLog.d(tag, "check package user=$userId package=$packageName code=${result.exitCode} hasPackage=$installed")
        return installed
    }

    private fun runFirstSuccessful(vararg commands: String): String {
        return runFirstSuccessfulResult(*commands).output
    }

    private fun runFirstSuccessfulResult(vararg commands: String): ShellResult {
        commands.forEach { command ->
            val result = PrivilegedShell.run(settingsRepository, command)
            if (result.exitCode == 0) return result
        }
        return ShellResult(-1, "")
    }

    private fun parseApps(output: String, userId: Int): List<SystemLinkApp> {
        val apps = mutableListOf<SystemLinkApp>()
        var packageName = ""
        val domainLines = mutableListOf<String>()
        var inTargetUser = false
        var inSelectionState = false

        fun flush() {
            if (packageName.isBlank()) return
            val domains = parseDomains(domainLines.joinToString("\n"))
            if (domains.isNotEmpty()) {
                apps += SystemLinkApp(
                    packageName = packageName,
                    label = appLabel(packageName),
                    linkHandlingAllowed = parseLinkHandlingAllowed(domainLines),
                    domains = domains,
                )
            }
            domainLines.clear()
            inTargetUser = false
            inSelectionState = false
        }

        output.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            val detectedPackageName = rawLine.extractPackageHeader()
            if (detectedPackageName != null) {
                flush()
                packageName = detectedPackageName
                inTargetUser = false
                inSelectionState = false
                return@forEach
            }
            if (line == "Domain verification state:") {
                inTargetUser = false
                inSelectionState = false
                return@forEach
            }
            if (line.startsWith("User ") && line.endsWith(":")) {
                inTargetUser = line.removePrefix("User ").removeSuffix(":").toIntOrNull() == userId
                inSelectionState = false
                return@forEach
            }
            if (line == "Selection state:") {
                inSelectionState = inTargetUser
                return@forEach
            }
            if (line.startsWith("Verification link handling allowed:")) {
                if (inTargetUser) domainLines += line
                return@forEach
            }
            if (line == "Disabled:" || line == "Enabled:") {
                if (inTargetUser) domainLines += line
                return@forEach
            }
            if (line.contains('.') && (line.contains(':') || inSelectionState)) {
                domainLines += line
            }
        }
        flush()
        return apps
    }

    private fun parseDomains(output: String): List<SystemLinkDomain> {
        var selectionEnabled: Boolean? = null
        var inSelectionList = false
        val domains = linkedMapOf<String, SystemLinkDomain>()
        output.lineSequence()
            .map { it.trim() }
            .forEach { line ->
                when (line) {
                    "Enabled:" -> {
                        selectionEnabled = true
                        inSelectionList = true
                        return@forEach
                    }
                    "Disabled:" -> {
                        selectionEnabled = false
                        inSelectionList = true
                        return@forEach
                    }
                }
                if (line.startsWith("Verification link handling allowed:")) return@forEach
                val normalized = line.removePrefix("*").trim()
                val parts = normalized.split(Regex("\\s+"), limit = 2)
                val host = parts.getOrNull(0).orEmpty().trimEnd(':')
                val state = if (inSelectionList) selectionEnabled?.let { if (it) "selected" else "disabled" }.orEmpty()
                else parts.getOrNull(1)?.trim()?.trimStart(':')?.trim().orEmpty()
                if (!host.contains('.') || host.equals("Domains", true)) return@forEach
                if (state.isBlank()) return@forEach
                val existing = domains[host]
                if (existing?.state?.isVerifiedSystemLinkState() == true && inSelectionList) return@forEach
                val displayState = if (state.isVerifiedSystemLinkState()) "verified" else state
                val domain = SystemLinkDomain(host = host, enabled = displayState.isSystemLinkEnabled(), state = displayState)
                domains[host] = when {
                    inSelectionList -> domain
                    existing == null -> domain
                    else -> existing
                }
            }
        return domains.values
            .sortedBy { it.host }
            .toList()
    }

    private fun parseLinkHandlingAllowed(lines: List<String>): Boolean {
        return lines.firstOrNull { it.startsWith("Verification link handling allowed:") }
            ?.substringAfter(':')
            ?.trim()
            ?.equals("true", ignoreCase = true)
            ?: true
    }

    private fun String.isSystemLinkEnabled(): Boolean {
        val value = lowercase()
        return value.contains("verified") || value.contains("approved") || value.contains("selected") || value.contains("enabled")
    }

    private fun String.isVerifiedSystemLinkState(): Boolean = equals("verified", ignoreCase = true)

    private fun String.extractPackageHeader(): String? {
        val indent = takeWhile { it.isWhitespace() }.length
        if (indent > APP_HEADER_MAX_INDENT) return null
        val line = trim().removePrefix("Package:").trim().trimEnd(':').trim()
        val packageName = line.substringBefore(' ').trim()
        val suffix = line.removePrefix(packageName).trim()
        if (!PACKAGE_NAME_REGEX.matches(packageName)) return null
        if (suffix.isNotEmpty() && !UUID_REGEX.matches(suffix)) return null
        return packageName
    }

    private fun appLabel(packageName: String): String {
        return runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }

    private fun shellQuote(value: String): String = IntentAmStartCommand.shellQuote(value)

    private companion object {
        const val APP_HEADER_MAX_INDENT = 4
        val PACKAGE_NAME_REGEX = Regex("""[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+""")
        val UUID_REGEX = Regex("""[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}""")
        const val PACKAGE_INSTALL_CACHE_MILLIS = 30_000L
        val packageInstallCache = ConcurrentHashMap<String, PackageInstallCacheEntry>()
    }

    private data class PackageInstallCacheEntry(val installed: Boolean, val checkedAt: Long)
}
