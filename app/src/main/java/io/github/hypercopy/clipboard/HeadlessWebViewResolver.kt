package io.github.hypercopy.clipboard

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

object HeadlessWebViewResolver {
    private const val TAG = "HyperCopy"
    private const val TIMEOUT_MILLIS = 3_000L
    private val handler = Handler(Looper.getMainLooper())

    fun resolveAndLaunch(context: Context, url: String, packageName: String) {
        handler.post {
            Resolver(context.applicationContext, url, packageName).start()
        }
    }

    private class Resolver(
        private val context: Context,
        private val url: String,
        private val packageName: String,
    ) {
        private var finished = false
        private var webView: WebView? = null
        private val timeoutRunnable = Runnable { fallback() }

        @SuppressLint("SetJavaScriptEnabled")
        fun start() {
            val view = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        return handleUrl(request.url.toString())
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        return handleUrl(url)
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        view.evaluateJavascript(AUTO_CLICK_ONCE_JS, null)
                    }
                }
            }
            webView = view
            handler.postDelayed(timeoutRunnable, TIMEOUT_MILLIS)
            Log.d(TAG, "headless webview load: $url")
            view.loadUrl(url)
        }

        private fun handleUrl(nextUrl: String): Boolean {
            if (isWebUrl(nextUrl)) return false
            finishWithLaunch(nextUrl, "")
            return true
        }

        private fun fallback() {
            if (finished) return
            finishWithLaunch(url, packageName)
        }

        private fun finishWithLaunch(targetUrl: String, targetPackageName: String) {
            if (finished) return
            finished = true
            handler.removeCallbacks(timeoutRunnable)
            Log.d(TAG, "headless webview resolved: $targetUrl")
            RootActivityLauncher.launch(targetUrl.toViewIntent(targetPackageName).withResolvedActivity(context.packageManager))
            webView?.runCatchingDestroy()
            webView = null
        }

        private fun WebView.runCatchingDestroy() {
            runCatching {
                stopLoading()
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
    }
}

private fun isWebUrl(url: String): Boolean = url.startsWith("http://", true) || url.startsWith("https://", true)

private const val AUTO_CLICK_ONCE_JS = """
(function() {
  var nodes = Array.prototype.slice.call(document.querySelectorAll('a,button,[role="button"]'));
  for (var i = 0; i < nodes.length; i++) {
    var node = nodes[i];
    var text = (node.innerText || node.textContent || node.getAttribute('aria-label') || '').toLowerCase();
    var rect = node.getBoundingClientRect();
    var style = window.getComputedStyle(node);
    if (rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none') {
      if (text.indexOf('打开') >= 0 || text.indexOf('app') >= 0 || text.indexOf('open') >= 0) {
        node.click();
        return true;
      }
    }
  }
  for (var j = 0; j < nodes.length; j++) {
    var fallback = nodes[j];
    var fallbackRect = fallback.getBoundingClientRect();
    var fallbackStyle = window.getComputedStyle(fallback);
    if (fallbackRect.width > 0 && fallbackRect.height > 0 && fallbackStyle.visibility !== 'hidden' && fallbackStyle.display !== 'none') {
      fallback.click();
      return true;
    }
  }
  return false;
})();
"""
