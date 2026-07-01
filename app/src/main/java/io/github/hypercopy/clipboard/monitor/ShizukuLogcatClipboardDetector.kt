package io.github.hypercopy.clipboard.monitor

import io.github.hypercopy.HyperLog
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class ShizukuLogcatClipboardDetector(
    private val packageName: String,
    private val processStarter: (Array<String>) -> Process?,
    private val onClipboardChanged: () -> Unit,
) {
    private val running = AtomicBoolean(false)
    private var process: Process? = null
    private var worker: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        worker = Thread(::readLoop, "HyperCopy-ShizukuLogcat").also { it.start() }
    }

    fun stop() {
        running.set(false)
        process?.destroy()
        process = null
        worker = null
    }

    private fun readLoop() {
        runCatching {
            val since = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            HyperLog.d(TAG, "start Shizuku logcat clipboard detector")
            process = processStarter(arrayOf("logcat", "-T", since, "ClipboardService:E", "*:S")) ?: return
            process?.inputStream?.bufferedReader()?.use(::readLines)
        }.onFailure { throwable ->
            if (running.get()) HyperLog.d(TAG, "Shizuku logcat detector failed", throwable)
        }
        running.set(false)
    }

    private fun readLines(reader: BufferedReader) {
        while (running.get()) {
            val line = reader.readLine() ?: break
            if (line.contains(packageName) && line.contains("Clipboard", ignoreCase = true)) {
                HyperLog.d(TAG, "Shizuku detected clipboard log: $line")
                onClipboardChanged()
            }
        }
    }

    private companion object {
        const val TAG = "HyperCopy"
    }
}
