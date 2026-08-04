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
    @Volatile
    private var currentStatus = Status.Stopped

    enum class Status {
        Checking,
        RunningShizuku,
        RunningReadLogs,
        PermissionDenied,
        Unavailable,
        Stopped,
    }

    fun start(context: Context, onStatusChanged: (Status) -> Unit = {}) {
        val appContext = context.applicationContext
        if (detector != null) {
            onStatusChanged(currentStatus)
            return
        }
        val generation = ++startGeneration
        updateStatus(Status.Checking, onStatusChanged)
        startProbe(appContext)
        ShizukuPermission.waitForAvailable { available ->
            if (generation != startGeneration) return@waitForAvailable
            if (available) {
                startWithShizuku(appContext, generation, onStatusChanged)
            } else {
                startWithReadLogsFallback(appContext, generation, onStatusChanged)
            }
        }
    }

    private fun startWithShizuku(appContext: Context, generation: Int, onStatusChanged: (Status) -> Unit) {
        if (detector != null) return
        ShizukuPermission.requestIfNeeded { granted ->
            if (generation != startGeneration) return@requestIfNeeded
            if (granted) {
                startDetector(appContext, generation, Status.RunningShizuku, onStatusChanged) { command -> ShizukuProcess.start(command) }
            } else {
                updateStatus(Status.PermissionDenied, onStatusChanged)
                Toast.makeText(appContext, R.string.toast_shizuku_permission_denied, Toast.LENGTH_SHORT).show()
                HyperLog.d(TAG, "Shizuku permission denied")
            }
        }
    }

    private fun startWithReadLogsFallback(appContext: Context, generation: Int, onStatusChanged: (Status) -> Unit) {
        if (detector != null) return
        if (hasReadLogsPermission(appContext)) {
            startDetector(appContext, generation, Status.RunningReadLogs, onStatusChanged) { command -> Runtime.getRuntime().exec(command) }
        } else {
            updateStatus(Status.Unavailable, onStatusChanged)
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
        currentStatus = Status.Stopped
        HyperLog.d(TAG, "stop Shizuku clipboard monitor")
    }

    private fun startProbe(context: Context) {
        if (probe != null) return
        probe = ClipboardChangeProbe(context).also { it.start() }
    }

    private fun startDetector(
        context: Context,
        generation: Int,
        runningStatus: Status,
        onStatusChanged: (Status) -> Unit,
        processStarter: (Array<String>) -> Process?,
    ) {
        if (detector != null) return
        detector = ShizukuLogcatClipboardDetector(
            packageName = context.packageName,
            processStarter = processStarter,
            onRunningChanged = { running ->
                if (generation == startGeneration) {
                    updateStatus(if (running) runningStatus else Status.Stopped, onStatusChanged)
                }
            },
        ) {
            ClipboardFocusRequester.request(context)
        }.also { it.start() }
    }

    private fun updateStatus(status: Status, onStatusChanged: (Status) -> Unit) {
        currentStatus = status
        onStatusChanged(status)
    }

    private fun hasReadLogsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED
    }
}
