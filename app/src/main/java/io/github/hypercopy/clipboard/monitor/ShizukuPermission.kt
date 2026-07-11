package io.github.hypercopy.clipboard.monitor

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import io.github.hypercopy.HyperLog
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicBoolean

object ShizukuPermission {
    private const val TAG = "HyperCopy"
    private const val REQUEST_CODE = 3001
    private const val BINDER_WAIT_TIMEOUT_MS = 3_000L
    private val permissionLock = Any()
    private val pendingPermissionResults = mutableListOf<(Boolean) -> Unit>()
    private var permissionRequestInFlight = false

    fun isAvailable(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun isGranted(): Boolean {
        if (!isAvailable()) return false
        return runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
    }

    fun waitForAvailable(onResult: (Boolean) -> Unit) {
        if (isAvailable()) {
            onResult(true)
            return
        }

        val completed = AtomicBoolean(false)
        val mainHandler = Handler(Looper.getMainLooper())
        lateinit var listener: Shizuku.OnBinderReceivedListener
        fun finish(available: Boolean) {
            if (!completed.compareAndSet(false, true)) return
            Shizuku.removeBinderReceivedListener(listener)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                onResult(available)
            } else {
                mainHandler.post { onResult(available) }
            }
        }

        listener = Shizuku.OnBinderReceivedListener { finish(true) }
        Shizuku.addBinderReceivedListener(listener)
        mainHandler.postDelayed({ finish(isAvailable()) }, BINDER_WAIT_TIMEOUT_MS)
    }

    fun requestIfNeeded(onResult: (Boolean) -> Unit) {
        if (!isAvailable()) {
            onResult(false)
            return
        }
        if (isGranted()) {
            onResult(true)
            return
        }

        synchronized(permissionLock) {
            pendingPermissionResults += onResult
            if (permissionRequestInFlight) return
            permissionRequestInFlight = true
        }

        val mainHandler = Handler(Looper.getMainLooper())
        fun dispatchResult(granted: Boolean) {
            val dispatch = {
                val callbacks = synchronized(permissionLock) {
                    permissionRequestInFlight = false
                    pendingPermissionResults.toList().also { pendingPermissionResults.clear() }
                }
                callbacks.forEach { it(granted) }
            }
            if (Looper.myLooper() == Looper.getMainLooper()) dispatch() else mainHandler.post(dispatch)
        }

        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode != REQUEST_CODE) return
                Shizuku.removeRequestPermissionResultListener(this)
                dispatchResult(grantResult == PackageManager.PERMISSION_GRANTED)
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }.onFailure { throwable ->
            HyperLog.d(TAG, "request Shizuku permission failed", throwable)
            Shizuku.removeRequestPermissionResultListener(listener)
            dispatchResult(false)
        }
    }
}
