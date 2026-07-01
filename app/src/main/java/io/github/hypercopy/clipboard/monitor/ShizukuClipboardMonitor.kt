package io.github.hypercopy.clipboard.monitor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.github.hypercopy.HyperLog
import io.github.hypercopy.R

object ShizukuClipboardMonitor {
    private const val TAG = "HyperCopy"

    private var detector: ShizukuLogcatClipboardDetector? = null
    private var probe: ClipboardChangeProbe? = null
    private var startGeneration = 0

    fun start(context: Context) {
        val appContext = context.applicationContext
        val generation = ++startGeneration
        startProbe(appContext)
        ShizukuPermission.waitForAvailable { available ->
            if (generation != startGeneration) return@waitForAvailable
            if (available) {
                startWithShizuku(appContext, generation)
            } else {
                startWithReadLogsFallback(appContext)
            }
        }
    }

    private fun startWithShizuku(appContext: Context, generation: Int) {
        if (detector != null) return
        ShizukuPermission.requestIfNeeded { granted ->
            if (generation != startGeneration) return@requestIfNeeded
            if (granted) {
                startDetector(appContext) { command -> ShizukuProcess.start(command) }
            } else {
                Toast.makeText(appContext, R.string.toast_shizuku_permission_denied, Toast.LENGTH_SHORT).show()
                HyperLog.d(TAG, "Shizuku permission denied")
            }
        }
    }

    private fun startWithReadLogsFallback(appContext: Context) {
        if (detector != null) return
        if (hasReadLogsPermission(appContext)) {
            startDetector(appContext) { command -> Runtime.getRuntime().exec(command) }
        } else {
            Toast.makeText(appContext, R.string.toast_shizuku_unavailable, Toast.LENGTH_SHORT).show()
            HyperLog.d(TAG, "Shizuku unavailable and READ_LOGS not granted")
        }
    }

    fun stop() {
        startGeneration++
        detector?.stop()
        detector = null
        probe?.stop()
        probe = null
        HyperLog.d(TAG, "stop Shizuku clipboard monitor")
    }

    private fun startProbe(context: Context) {
        if (probe != null) return
        probe = ClipboardChangeProbe(context).also { it.start() }
    }

    private fun startDetector(context: Context, processStarter: (Array<String>) -> Process?) {
        if (detector != null) return
        detector = ShizukuLogcatClipboardDetector(
            packageName = context.packageName,
            processStarter = processStarter,
        ) {
            ClipboardFocusRequester.request(context)
        }.also { it.start() }
    }

    private fun hasReadLogsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED
    }
}
