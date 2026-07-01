package io.github.hypercopy.hook;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ClipboardManager;
import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.hypercopy.Config;
import io.github.libxposed.api.XposedModule;

public class HookEntry extends XposedModule {
    private static final String TAG = "HyperCopy";
    private static final String CLIPBOARD_SERVICE_CLASS = "com.android.server.clipboard.ClipboardService";
    private static final String RECEIVER_CLASS = "io.github.hypercopy.clipboard.ClipboardTextReceiver";
    private static final long DUPLICATE_WINDOW_MILLIS = 1500L;
    private static final int INSTALL_RETRY_LIMIT = 20;
    private static final long INSTALL_RETRY_DELAY_MILLIS = 1000L;
    private static final int CLEAR_RECEIVER_RETRY_LIMIT = 30;
    private static final long CLEAR_RECEIVER_RETRY_DELAY_MILLIS = 1000L;

    private static String lastText = "";
    private static long lastSentAt = 0L;
    private boolean hooksInstalled = false;
    private boolean clearReceiverRegistered = false;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        logDebug("module loaded: process=" + param.getProcessName()
            + ", systemServer=" + param.isSystemServer()
            + ", api=" + getApiVersion());
    }

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        installClipboardHooksWithRetry(param.getClassLoader(), "onSystemServerStarting", 0);
    }

    private void installClipboardHooksWithRetry(ClassLoader classLoader, String source, int attempt) {
        if (hooksInstalled) return;
        if (installClipboardHooks(classLoader, source + ", attempt=" + attempt)) return;
        if (attempt >= INSTALL_RETRY_LIMIT) return;
        new Handler(Looper.getMainLooper()).postDelayed(
            () -> installClipboardHooksWithRetry(classLoader, source, attempt + 1),
            INSTALL_RETRY_DELAY_MILLIS
        );
    }

    private boolean installClipboardHooks(ClassLoader classLoader, String source) {
        if (hooksInstalled) {
            logDebug("ClipboardService hooks already installed, source=" + source);
            return true;
        }
        logDebug("installing ClipboardService hooks, source=" + source);
        try {
            Class<?> clipboardServiceClass = Class.forName(CLIPBOARD_SERVICE_CLASS, false, classLoader);
            int hookedCount = 0;
            for (Method method : clipboardServiceClass.getDeclaredMethods()) {
                if (!isSetPrimaryClipMethod(method)) continue;
                method.setAccessible(true);
                logDebug("hook ClipboardService method: " + method.toGenericString());
                hook(method).setId("hypercopy_clipboard_" + method.toGenericString()).intercept(chain -> {
                    Object result = chain.proceed();
                    try {
                        ClipData clipData = findClipData(chain.getArgs().toArray());
                        Context context = findContext(chain.getThisObject());
                        sendTextIfNeeded(context, clipData, chain.getArgs().toArray());
                    } catch (Throwable throwable) {
                        logWarn("clipboard hook callback failed", throwable);
                    }
                    return result;
                });
                hookedCount++;
            }
            hooksInstalled = hookedCount > 0;
            if (hooksInstalled) registerClearReceiverWithRetry(0);
            logDebug("ClipboardService hooks installed: " + hookedCount);
            return hooksInstalled;
        } catch (ClassNotFoundException throwable) {
            logDebug("ClipboardService not ready, source=" + source + ", classLoader=" + classLoader);
            return false;
        } catch (Throwable throwable) {
            logError("Failed to hook ClipboardService", throwable);
            return false;
        }
    }

    private static boolean isSetPrimaryClipMethod(Method method) {
        if (!method.getName().startsWith("setPrimaryClip")) return false;
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (ClipData.class.isAssignableFrom(parameterType)) return true;
        }
        return false;
    }

    private static ClipData findClipData(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof ClipData) return (ClipData) arg;
        }
        return null;
    }

    private static Context findContext(Object service) {
        if (service == null) return null;
        Class<?> current = service.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (!Context.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(service);
                    if (value instanceof Context) return (Context) value;
                } catch (Throwable ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Context findSystemContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object activityThread = currentActivityThread.invoke(null);
            if (activityThread == null) return null;
            Method getSystemContext = activityThreadClass.getDeclaredMethod("getSystemContext");
            getSystemContext.setAccessible(true);
            Object context = getSystemContext.invoke(activityThread);
            if (context instanceof Context) return (Context) context;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void registerClearReceiverWithRetry(int attempt) {
        if (clearReceiverRegistered) return;
        if (registerClearReceiver(findSystemContext(), attempt)) return;
        if (attempt >= CLEAR_RECEIVER_RETRY_LIMIT) return;
        new Handler(Looper.getMainLooper()).postDelayed(
            () -> registerClearReceiverWithRetry(attempt + 1),
            CLEAR_RECEIVER_RETRY_DELAY_MILLIS
        );
    }

    private boolean registerClearReceiver(Context context, int attempt) {
        if (clearReceiverRegistered) return true;
        if (context == null) {
            logDebug("clipboard clear receiver context not ready, attempt=" + attempt);
            return false;
        }
        try {
            IntentFilter filter = new IntentFilter(Config.ACTION_CLEAR_CLIPBOARD);
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!Config.ACTION_CLEAR_CLIPBOARD.equals(intent.getAction())) return;
                    try {
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            clipboard.clearPrimaryClip();
                        } else {
                            clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
                        }
                        setResultCode(Activity.RESULT_OK);
                        logDebug("clipboard cleared in system_server by LSPosed");
                    } catch (Throwable throwable) {
                        setResultCode(Activity.RESULT_CANCELED);
                        logWarn("clear clipboard in system_server failed", throwable);
                    }
                }
            }, filter, Config.PERMISSION_CLEAR_CLIPBOARD, null, Context.RECEIVER_EXPORTED);
            clearReceiverRegistered = true;
            logDebug("clipboard clear receiver registered");
            return true;
        } catch (Throwable throwable) {
            logWarn("register clipboard clear receiver failed, attempt=" + attempt, throwable);
            return false;
        }
    }

    private void sendTextIfNeeded(Context context, ClipData clipData, Object[] args) {
        if (context == null || clipData == null || clipData.getItemCount() == 0) return;
        CharSequence text = extractPlainText(context, clipData);
        if (text == null) return;

        String value = text.toString().trim();
        if (value.isEmpty() || value.length() > Config.CLIPBOARD_TEXT_MAX_LENGTH) return;

        long now = System.currentTimeMillis();
        if (value.equals(lastText) && now - lastSentAt < DUPLICATE_WINDOW_MILLIS) return;
        lastText = value;
        lastSentAt = now;
        String sourcePackage = findSourcePackage(context, args);

        Intent intent = new Intent(Config.ACTION_HANDLE_CLIPBOARD_TEXT)
            .setComponent(new ComponentName(Config.APPLICATION_ID, RECEIVER_CLASS))
            .putExtra(Config.EXTRA_CLIPBOARD_TEXT, value)
            .putExtra(Config.EXTRA_CLIPBOARD_SOURCE, sourcePackage)
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        logDebug("send clipboard text to app, length=" + value.length() + ", source=" + sourcePackage);
        context.sendBroadcast(intent);
    }

    private static String findSourcePackage(Context context, Object[] args) {
        if (args == null) return "";
        for (Object arg : args) {
            if (!(arg instanceof String)) continue;
            String value = ((String) arg).trim();
            if (isInstalledPackage(context, value)) return value;
        }
        for (Object arg : args) {
            if (!(arg instanceof Integer)) continue;
            int uid = (Integer) arg;
            if (uid < 10_000) continue;
            String[] packages = context.getPackageManager().getPackagesForUid(uid);
            if (packages != null && packages.length > 0) return packages[0];
        }
        return "";
    }

    private static boolean isInstalledPackage(Context context, String value) {
        if (value.isEmpty() || !value.contains(".") || value.contains(" ")) return false;
        try {
            context.getPackageManager().getApplicationInfo(value, 0);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static CharSequence extractPlainText(Context context, ClipData clipData) {
        ClipDescription description = clipData.getDescription();
        if (description == null) return null;
        boolean textMime = description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
            || description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML);
        if (!textMime) return null;

        ClipData.Item item = clipData.getItemAt(0);
        if (item == null || item.getUri() != null || item.getIntent() != null) return null;
        if (item.getText() != null) return item.getText();
        if (item.getHtmlText() != null) return item.getHtmlText();
        return item.coerceToText(context);
    }

    private void logDebug(String message) {
        Log.d(TAG, message);
        log(Log.DEBUG, TAG, message);
    }

    private void logWarn(String message, Throwable throwable) {
        Log.d(TAG, message, throwable);
        log(Log.WARN, TAG, message, throwable);
    }

    private void logError(String message, Throwable throwable) {
        Log.d(TAG, message, throwable);
        log(Log.ERROR, TAG, message, throwable);
    }
}
