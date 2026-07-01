package io.github.hypercopy.clipboard

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.hypercopy.Config
import io.github.hypercopy.HyperLog
import io.github.hypercopy.R
import io.github.hypercopy.clipboard.monitor.ClipboardFocusRequester
import io.github.hypercopy.data.SettingsRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit

object PendingJumpCoordinator {
    private const val TAG = "HyperCopy"
    private const val CHANNEL_ID = "hypercopy_jump_live"
    private const val NOTIFICATION_ID = 2001
    private const val EXPIRE_MILLIS = 5_000L
    private const val CLIPBOARD_CLEAR_TIMEOUT_MILLIS = 500L
    private const val SHIZUKU_FOREGROUND_CLEAR_TIMEOUT_MILLIS = 1_200L
    private val handler = Handler(Looper.getMainLooper())
    private val clipboardClearHandler = Handler(HandlerThread("HyperCopyClipboardClear").apply { start() }.looper)
    private val nextId = AtomicLong(1L)
    private var pending: Entry? = null

    fun submit(context: Context, jump: PendingJump, clearClipboardAfterJump: Boolean = false) {
        val appContext = context.applicationContext
        val notificationMode = SettingsRepository(appContext).readJumpNotificationMode()
        if (notificationMode == Config.JUMP_NOTIFICATION_MODE_NONE) {
            launch(appContext, jump, clearClipboardAfterJump)
            return
        }
        if (!canPostNotification(appContext)) {
            HyperLog.d(TAG, "jump notification permission missing, launch directly")
            launch(appContext, jump, clearClipboardAfterJump)
            return
        }

        val id = nextId.getAndIncrement()
        val entry = Entry(id, jump, clearClipboardAfterJump)
        entry.expireRunnable = Runnable { expire(appContext, id) }
        pending?.cancel(appContext)
        pending = entry
        createChannel(appContext)
        postNotification(appContext, entry, notificationMode)
        if (jump is PendingJump.WebViewJump) {
            entry.preload = HeadlessWebViewResolver.preload(
                appContext,
                jump.url,
                jump.packageName,
                clearClipboardAfterJump,
            )
        }
        handler.postDelayed(entry.expireRunnable, EXPIRE_MILLIS)
    }

    fun confirm(context: Context, id: Long) {
        val appContext = context.applicationContext
        val entry = pending ?: return
        if (entry.id != id) return
        pending = null
        NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
        handler.removeCallbacks(entry.expireRunnable)
        when (val jump = entry.jump) {
            is PendingJump.IntentJump -> launchAfterClipboardClear(appContext, entry.clearClipboardAfterJump) {
                ActivityLaunchStrategy.launch(appContext, jump.intent)
            }
            is PendingJump.WebViewJump -> entry.preload?.continueLaunch(appContext)
                ?: HeadlessWebViewResolver.resolveAndLaunch(appContext, jump.url, jump.packageName, entry.clearClipboardAfterJump)
        }
    }

    private fun expire(context: Context, id: Long) {
        val entry = pending ?: return
        if (entry.id != id) return
        pending = null
        entry.cancel(context)
        HyperLog.d(TAG, "jump notification expired")
    }

    private fun launch(context: Context, jump: PendingJump, clearClipboardAfterJump: Boolean) {
        when (jump) {
            is PendingJump.IntentJump -> launchAfterClipboardClear(context, clearClipboardAfterJump) {
                ActivityLaunchStrategy.launch(context, jump.intent)
            }
            is PendingJump.WebViewJump -> HeadlessWebViewResolver.resolveAndLaunch(context, jump.url, jump.packageName, clearClipboardAfterJump)
        }
    }

    private fun postNotification(context: Context, entry: Entry, notificationMode: String) {
        val actionIntent = Intent(context, JumpConfirmReceiver::class.java).apply {
            action = Config.ACTION_CONFIRM_JUMP
            putExtra(Config.EXTRA_PENDING_JUMP_ID, entry.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entry.id.toInt(),
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = context.getString(R.string.notification_jump_title)
        val content = entry.jump.title.ifBlank { context.getString(R.string.notification_jump_text) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_jump_text)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(EXPIRE_MILLIS)
            .addAction(android.R.drawable.ic_menu_view, context.getString(R.string.action_jump), pendingIntent)
            .requestPromotedOngoing()
            .build()
            .apply { flags = flags or Notification.FLAG_ONGOING_EVENT }
        if (notificationMode == Config.JUMP_NOTIFICATION_MODE_MIUI_ISLAND) {
            MiuiSuperIslandNotification.apply(context, notification, title, content, entry.jump.packageName, pendingIntent)
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    internal fun clearClipboardIfNeeded(context: Context, clearClipboardAfterJump: Boolean) {
        if (!clearClipboardAfterJump) return
        if (clearClipboardByLsposed(context)) return
        writeEmptyClipboardByApp(context)
    }

    internal fun launchAfterClipboardClear(context: Context, clearClipboardAfterJump: Boolean, launch: () -> Unit) {
        if (!clearClipboardAfterJump) {
            launch()
            return
        }
        if (clearClipboardByLsposed(context)) {
            launch()
            return
        }
        if (launchAfterShizukuForegroundClear(context, launch)) return
        clearClipboardIfNeeded(context, clearClipboardAfterJump)
        launch()
    }

    private fun clearClipboardByLsposed(context: Context): Boolean {
        if (SettingsRepository(context.applicationContext).readClipboardMonitorMode() != Config.CLIPBOARD_MONITOR_MODE_LSPOSED) return false
        val latch = CountDownLatch(1)
        var cleared = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                cleared = resultCode == android.app.Activity.RESULT_OK
                latch.countDown()
            }
        }
        return runCatching {
            context.applicationContext.sendOrderedBroadcast(
                Intent(Config.ACTION_CLEAR_CLIPBOARD)
                    .setPackage("android")
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND),
                Config.PERMISSION_CLEAR_CLIPBOARD,
                receiver,
                clipboardClearHandler,
                android.app.Activity.RESULT_CANCELED,
                null,
                null,
            )
            latch.await(CLIPBOARD_CLEAR_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            if (cleared) HyperLog.d(TAG, "clipboard cleared by LSPosed") else HyperLog.d(TAG, "LSPosed clipboard clear did not complete")
            cleared
        }.getOrElse { throwable ->
            HyperLog.d(TAG, "LSPosed clipboard clear exception", throwable)
            false
        }
    }

    private fun launchAfterShizukuForegroundClear(context: Context, launch: () -> Unit): Boolean {
        if (SettingsRepository(context.applicationContext).readClipboardMonitorMode() != Config.CLIPBOARD_MONITOR_MODE_SHIZUKU) return false
        var completed = false
        val token = ClipboardFocusRequester.requestClear(context.applicationContext) { success ->
            if (completed) return@requestClear
            completed = true
            if (success) {
                HyperLog.d(TAG, "clipboard replaced with empty text by Shizuku foreground")
            } else {
                writeEmptyClipboardByApp(context)
            }
            launch()
        } ?: return false
        handler.postDelayed({
            if (completed) return@postDelayed
            completed = true
            ClipboardFocusRequester.cancelClearToken(token)
            HyperLog.d(TAG, "Shizuku foreground clipboard clear timed out")
            writeEmptyClipboardByApp(context)
            launch()
        }, SHIZUKU_FOREGROUND_CLEAR_TIMEOUT_MILLIS)
        return true
    }

    private fun writeEmptyClipboardByApp(context: Context) {
        val clipboard = context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        HyperLog.d(TAG, "clipboard replaced with empty text by app")
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_jump_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_jump_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun NotificationCompat.Builder.requestPromotedOngoing(): NotificationCompat.Builder {
        runCatching {
            javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                .invoke(this, true)
        }.onFailure {
            extras.putBoolean("android.requestPromotedOngoing", true)
        }
        return this
    }

    private fun canPostNotification(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private data class Entry(
        val id: Long,
        val jump: PendingJump,
        val clearClipboardAfterJump: Boolean,
        var preload: HeadlessWebViewResolver.Preload? = null,
        var expireRunnable: Runnable = Runnable {},
    ) {
        fun cancel(context: Context) {
            handler.removeCallbacks(expireRunnable)
            preload?.cancel()
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }
}
