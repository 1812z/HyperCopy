package io.github.hypercopy.hook;

import android.content.SharedPreferences;
import android.util.Log;

import io.github.hypercopy.Config;
import io.github.libxposed.api.XposedInterface;

final class HookLog {
    private static volatile int logLevel = Config.DEFAULT_LOG_LEVEL;
    private static volatile SharedPreferences remotePreferences = null;
    private static final SharedPreferences.OnSharedPreferenceChangeListener LOG_LEVEL_LISTENER = (preferences, key) -> {
        if (key == null || Config.KEY_LOG_LEVEL.equals(key)) updateLogLevel(preferences);
    };

    private HookLog() {
    }

    static void init(XposedInterface xposed) {
        if (remotePreferences != null) return;
        try {
            SharedPreferences preferences = xposed.getRemotePreferences(Config.PREFS_NAME);
            updateLogLevel(preferences);
            preferences.registerOnSharedPreferenceChangeListener(LOG_LEVEL_LISTENER);
            remotePreferences = preferences;
        } catch (Throwable ignored) {
        }
    }

    static void d(XposedInterface xposed, String tag, String message) {
        log(xposed, Log.DEBUG, tag, message, null);
    }

    static void w(XposedInterface xposed, String tag, String message, Throwable throwable) {
        log(xposed, Log.WARN, tag, message, throwable);
    }

    static void e(XposedInterface xposed, String tag, String message, Throwable throwable) {
        log(xposed, Log.ERROR, tag, message, throwable);
    }

    private static void log(XposedInterface xposed, int priority, String tag, String message, Throwable throwable) {
        init(xposed);
        if (logLevel < requiredLogLevel(priority)) return;
        if (throwable == null) {
            Log.println(priority, tag, message);
            xposed.log(priority, tag, message);
        } else {
            Log.println(priority, tag, message + '\n' + Log.getStackTraceString(throwable));
            xposed.log(priority, tag, message, throwable);
        }
    }

    private static int requiredLogLevel(int priority) {
        return priority <= Log.DEBUG ? Config.LOG_LEVEL_DEBUG : Config.LOG_LEVEL_BASIC;
    }

    private static void updateLogLevel(SharedPreferences preferences) {
        logLevel = preferences.getInt(Config.KEY_LOG_LEVEL, Config.DEFAULT_LOG_LEVEL);
    }
}
