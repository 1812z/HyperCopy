package io.github.hypercopy.ui.pages.rules

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.hypercopy.R
import io.github.hypercopy.data.rules.RuleCategory
import io.github.hypercopy.ui.activities.RuleEditorActivity
import io.github.hypercopy.ui.components.isWebUrl
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RuleBrowserPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var pageUrl by remember { mutableStateOf(DEFAULT_URL) }
    var address by remember { mutableStateOf(DEFAULT_URL) }
    var intercepted by remember { mutableStateOf<InterceptedJump?>(null) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(modifier = Modifier.size(42.dp), onClick = onBack) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = stringResource(R.string.action_back))
                    }
                }
                TextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = stringResource(R.string.browser_hint_url),
                )
                TextButton(text = stringResource(R.string.action_open), onClick = { pageUrl = normalizeUrl(address) })
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LearningWebView(
                    url = pageUrl,
                    onPageUrlChange = {
                        pageUrl = it
                        address = it
                    },
                    onAppJump = { targetUrl -> intercepted = InterceptedJump(pageUrl, targetUrl) },
                )
                intercepted?.let { jump ->
                    InterceptedJumpSheet(
                        jump = jump,
                        onDismiss = { intercepted = null },
                        onAddRule = { sourceUrl, targetUrl ->
                            context.startActivity(
                                Intent(context, RuleEditorActivity::class.java)
                                    .putExtra(RuleEditorActivity.EXTRA_CATEGORY, RuleCategory.Link.value)
                                    .putExtra(RuleEditorActivity.EXTRA_SOURCE_URL, sourceUrl)
                                    .putExtra(RuleEditorActivity.EXTRA_TARGET_URL, targetUrl),
                            )
                            intercepted = null
                            onBack()
                        },
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LearningWebView(url: String, onPageUrlChange: (String) -> Unit, onAppJump: (String) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.userAgentString = DESKTOP_USER_AGENT
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val nextUrl = request.url.toString()
                        if (isWebUrl(nextUrl)) return false
                        onAppJump(nextUrl)
                        return true
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        if (isWebUrl(url)) return false
                        onAppJump(url)
                        return true
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        onPageUrlChange(url)
                    }
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) webView.loadUrl(url)
        },
    )
}

@Composable
private fun InterceptedJumpSheet(jump: InterceptedJump, onDismiss: () -> Unit, onAddRule: (String, String) -> Unit) {
    var sourceUrl by remember(jump) { mutableStateOf(jump.sourceUrl) }
    var targetUrl by remember(jump) { mutableStateOf(jump.targetUrl) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Card(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Column(
                modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = stringResource(R.string.browser_dialog_jump_detected), style = MiuixTheme.textStyles.title3)
                Text(text = stringResource(R.string.browser_dialog_jump_hint), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                DraftField(stringResource(R.string.browser_label_web_page), sourceUrl, onValueChange = { sourceUrl = it })
                DraftField(stringResource(R.string.browser_label_jump), targetUrl, onValueChange = { targetUrl = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(text = stringResource(R.string.action_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
                    TextButton(
                        text = stringResource(R.string.action_add_rule),
                        onClick = { onAddRule(sourceUrl, targetUrl) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftField(title: String, content: String, onValueChange: (String) -> Unit, singleLine: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MiuixTheme.textStyles.headline1)
        TextField(
            value = content,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 3,
        )
    }
}

private data class InterceptedJump(val sourceUrl: String, val targetUrl: String)

private fun normalizeUrl(text: String): String {
    val value = text.trim()
    return if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
}

private const val DEFAULT_URL = "https://www.bing.com"
private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36"
