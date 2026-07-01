package io.github.hypercopy.clipboard.monitor

import android.content.pm.PackageManager
import io.github.hypercopy.HyperLog
import rikka.shizuku.Shizuku

object ShizukuPermission {
    private const val TAG = "HyperCopy"
    private const val REQUEST_CODE = 3001

    fun isAvailable(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun isGranted(): Boolean {
        if (!isAvailable()) return false
        return runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
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

        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode != REQUEST_CODE) return
                Shizuku.removeRequestPermissionResultListener(this)
                onResult(grantResult == PackageManager.PERMISSION_GRANTED)
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }.onFailure { throwable ->
            HyperLog.d(TAG, "request Shizuku permission failed", throwable)
            Shizuku.removeRequestPermissionResultListener(listener)
            onResult(false)
        }
    }
}
