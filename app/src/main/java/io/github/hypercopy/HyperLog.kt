package io.github.hypercopy

import android.content.Context
import android.util.Log

object HyperLog {
    @Volatile
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    fun d(tag: String, message: String) {
        if (logLevel() >= Config.LOG_LEVEL_DEBUG) Log.d(tag, message)
    }

    fun d(tag: String, message: String, throwable: Throwable) {
        if (logLevel() >= Config.LOG_LEVEL_DEBUG) Log.d(tag, message, throwable)
    }

    fun w(tag: String, message: String) {
        if (logLevel() >= Config.LOG_LEVEL_BASIC) Log.w(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable) {
        if (logLevel() >= Config.LOG_LEVEL_BASIC) Log.w(tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        if (logLevel() >= Config.LOG_LEVEL_BASIC) Log.e(tag, message, throwable)
    }

    private fun logLevel(): Int {
        val appContext = context ?: return Config.DEFAULT_LOG_LEVEL
        return appContext
            .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(Config.KEY_LOG_LEVEL, Config.DEFAULT_LOG_LEVEL)
    }
}
