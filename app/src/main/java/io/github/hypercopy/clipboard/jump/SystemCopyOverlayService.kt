package io.github.hypercopy.clipboard.jump

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.hypercopy.HyperLog
import io.github.hypercopy.clipboard.privileged.ActivityLaunchStrategy

class SystemCopyOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlay: LinearLayout? = null
    private var dismissInProgress = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        removeOverlay()
        if (intent == null || !Settings.canDrawOverlays(this)) {
            HyperLog.d(TAG, "system copy overlay unavailable: overlay permission missing")
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "目标应用" }
        val packageName = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val targetUri = intent.getStringExtra(EXTRA_URI)
        showOverlay(title, packageName, targetUri)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay(title: String, packageName: String, targetUri: String?) {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        val text = TextView(this).apply {
            setTextColor(if (nightMode) Color.rgb(245, 245, 245) else Color.rgb(34, 34, 34))
            textSize = 15f
            maxLines = 1
            maxWidth = dp(234)
            ellipsize = TextUtils.TruncateAt.END
            this.text = "打开$title"
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(if (nightMode) Color.rgb(36, 36, 36) else Color.WHITE)
                cornerRadius = dp(18).toFloat()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) isForceDarkAllowed = false
            packageIcon(packageName)?.let { drawable ->
                addView(ImageView(this@SystemCopyOverlayService).apply {
                    setImageDrawable(drawable)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }, LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                    marginEnd = dp(6)
                })
            }
            addView(text, LinearLayout.LayoutParams(-2, -2))
            setOnClickListener {
                launchTarget(packageName, targetUri)
                dismissOverlay()
            }
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            translationX = dp(18).toFloat()
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dp(12)
            y = dp(116)
        }
        runCatching {
            windowManager = getSystemService(WindowManager::class.java)
            windowManager?.addView(root, params)
            overlay = root
            root.post {
                root.pivotX = root.width.toFloat()
                root.pivotY = root.height / 2f
                root.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationX(0f)
                    .setDuration(ENTER_ANIMATION_MILLIS)
                    .setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f))
                    .start()
            }
            HyperLog.d(TAG, "system copy overlay shown for $packageName, uri=$targetUri")
            handler.postDelayed(::dismissOverlay, DISPLAY_MILLIS)
        }.onFailure { throwable ->
            HyperLog.d(TAG, "system copy overlay failed", throwable)
            stopSelf()
        }
    }

    private fun launchTarget(packageName: String, targetUri: String?) {
        runCatching {
            val target = if (!targetUri.isNullOrBlank()) {
                Intent.parseUri(targetUri, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW).setData(Uri.parse(targetUri.orEmpty()))
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (packageName.isNotBlank()) setPackage(packageName)
            }
            ActivityLaunchStrategy.launch(this, target, null)
        }.onFailure { throwable ->
            HyperLog.d(TAG, "system copy overlay launch failed", throwable)
        }
    }

    private fun removeOverlay() {
        handler.removeCallbacksAndMessages(null)
        overlay?.let { view -> runCatching { windowManager?.removeView(view) } }
        overlay = null
        dismissInProgress = false
    }

    private fun dismissOverlay() {
        val view = overlay ?: run {
            stopSelf()
            return
        }
        if (dismissInProgress) return
        dismissInProgress = true
        handler.removeCallbacksAndMessages(null)
        view.animate().cancel()
        view.pivotX = view.width.toFloat()
        view.pivotY = view.height / 2f
        view.animate()
            .alpha(0f)
            .scaleX(0.92f)
            .scaleY(0.92f)
            .translationX(dp(18).toFloat())
            .setDuration(EXIT_ANIMATION_MILLIS)
            .setInterpolator(PathInterpolator(0.4f, 0f, 1f, 1f))
            .withEndAction {
                removeOverlay()
                stopSelf()
            }
            .start()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun packageIcon(packageName: String) = runCatching {
        packageManager.getApplicationIcon(packageName)
    }.getOrNull()

    companion object {
        private const val TAG = "HyperCopy"
        private const val DISPLAY_MILLIS = 6_000L
        private const val ENTER_ANIMATION_MILLIS = 200L
        private const val EXIT_ANIMATION_MILLIS = 160L
        const val EXTRA_TITLE = "title"
        const val EXTRA_PACKAGE = "package_name"
        const val EXTRA_URI = "target_uri"

        fun start(context: android.content.Context, jump: PendingJump): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return false
            val targetUri = when (jump) {
                is PendingJump.IntentJump -> jump.intent.toUri(Intent.URI_INTENT_SCHEME)
                is PendingJump.WebViewJump -> jump.url
                is PendingJump.SystemLinkJump -> jump.url
            }
            val launchIntent = Intent(context, SystemCopyOverlayService::class.java).apply {
                putExtra(EXTRA_TITLE, appLabel(context, jump.packageName))
                putExtra(EXTRA_PACKAGE, jump.packageName)
                putExtra(EXTRA_URI, targetUri)
            }
            return runCatching {
                context.startService(launchIntent)
                true
            }.getOrElse { false }
        }

        private fun appLabel(context: android.content.Context, packageName: String): String {
            if (packageName.isBlank()) return "目标应用"
            return runCatching {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(info).toString()
            }.getOrDefault(packageName)
        }
    }
}
