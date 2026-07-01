package io.github.hypercopy.clipboard

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.hypercopy.HyperLog
import java.util.concurrent.atomic.AtomicBoolean

object HeadlessWebViewResolver {
    private const val TAG = "HyperCopy"
    private const val TIMEOUT_MILLIS = 3_000L
    private val handler = Handler(Looper.getMainLooper())

    fun resolveAndLaunch(context: Context, url: String, packageName: String, clearClipboardAfterJump: Boolean = false) {
        handler.post {
            Resolver(context.applicationContext, url, packageName, clearClipboardAfterJump, launchWhenResolved = true).start()
        }
    }

    fun preload(context: Context, url: String, packageName: String, clearClipboardAfterJump: Boolean = false): Preload {
        val preload = Preload(context.applicationContext, url, packageName, clearClipboardAfterJump)
        handler.post { preload.start() }
        return preload
    }

    class Preload internal constructor(
        private val context: Context,
        private val url: String,
        private val packageName: String,
        private val clearClipboardAfterJump: Boolean,
    ) {
        private val confirmed = AtomicBoolean(false)
        private var resolver: Resolver? = null
        private var resolvedIntent: android.content.Intent? = null

        internal fun start() {
            resolver = Resolver(context, url, packageName, clearClipboardAfterJump, launchWhenResolved = false) { intent ->
                resolvedIntent = intent
                if (confirmed.get() && ActivityLaunchStrategy.launch(context, intent)) {
                    PendingJumpCoordinator.clearClipboardIfNeeded(context, clearClipboardAfterJump)
                }
            }.also { it.start() }
        }

        fun continueLaunch(context: Context) {
            confirmed.set(true)
            handler.post {
                val intent = resolvedIntent
                if (intent != null) {
                    if (ActivityLaunchStrategy.launch(context.applicationContext, intent)) {
                        PendingJumpCoordinator.clearClipboardIfNeeded(context, clearClipboardAfterJump)
                    }
                }
            }
        }

        fun cancel() {
            handler.post {
                resolver?.cancel()
                resolver = null
            }
        }
    }

    private class Resolver(
        private val context: Context,
        private val url: String,
        private val packageName: String,
        private val clearClipboardAfterJump: Boolean,
        private val launchWhenResolved: Boolean,
        private val onResolved: ((android.content.Intent) -> Unit)? = null,
    ) {
        private var finished = false
        private var webView: WebView? = null
        private val timeoutRunnable = Runnable { fallback() }

        @SuppressLint("SetJavaScriptEnabled")
        fun start() {
            val view = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        return handleUrl(request.url.toString())
                    }

                    @Deprecated("Deprecated in Java")
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
            HyperLog.d(TAG, "headless webview load: $url")
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
            HyperLog.d(TAG, "headless webview resolved: $targetUrl")
            val intent = targetUrl.toViewIntent(targetPackageName)
            onResolved?.invoke(intent)
            if (launchWhenResolved && ActivityLaunchStrategy.launch(context, intent)) {
                PendingJumpCoordinator.clearClipboardIfNeeded(context, clearClipboardAfterJump)
            }
            webView?.runCatchingDestroy()
            webView = null
        }

        fun cancel() {
            if (finished) return
            finished = true
            handler.removeCallbacks(timeoutRunnable)
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
