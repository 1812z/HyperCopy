package io.github.hypercopy.ui.components

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HiddenWebViewResolver(
    url: String,
    onResolved: (String) -> Unit,
    onTimeout: () -> Unit,
    onPageLoaded: () -> Unit = {},
    timeoutMillis: Long = 2000L,
    modifier: Modifier = Modifier,
) {
    var finished by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        delay(timeoutMillis)
        if (!finished) {
            finished = true
            onTimeout()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val nextUrl = request.url.toString()
                        if (isWebUrl(nextUrl)) return false
                        if (!finished) {
                            finished = true
                            onResolved(nextUrl)
                        }
                        return true
                    }

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        if (isWebUrl(url)) return false
                        if (!finished) {
                            finished = true
                            onResolved(url)
                        }
                        return true
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        onPageLoaded()
                        view.evaluateJavascript(AUTO_CLICK_ONCE_JS, null)
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

fun isWebUrl(url: String): Boolean = url.startsWith("http://", true) || url.startsWith("https://", true)

private const val AUTO_CLICK_ONCE_JS = """
(function() {
  var nodes = Array.prototype.slice.call(document.querySelectorAll('a,button,[role="button"]'));
  for (var i = 0; i < nodes.length; i++) {
    var node = nodes[i];
    var rect = node.getBoundingClientRect();
    var style = window.getComputedStyle(node);
    if (rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none') {
      node.click();
      return true;
    }
  }
  return false;
})();
"""
