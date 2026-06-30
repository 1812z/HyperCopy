package io.github.hypercopy.hook;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

    private static String lastText = "";
    private static long lastSentAt = 0L;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        Log.i(TAG, "module loaded: process=" + param.getProcessName() + ", api=" + getApiVersion());
    }

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        try {
            Class<?> clipboardServiceClass = Class.forName(CLIPBOARD_SERVICE_CLASS, false, param.getClassLoader());
            int hookedCount = 0;
            for (Method method : clipboardServiceClass.getDeclaredMethods()) {
                if (!isSetPrimaryClipMethod(method)) continue;
                method.setAccessible(true);
                hook(method).setId("hypercopy_clipboard_" + method.toGenericString()).intercept(chain -> {
                    Object result = chain.proceed();
                    ClipData clipData = findClipData(chain.getArgs().toArray());
                    Context context = findContext(chain.getThisObject());
                    sendTextIfNeeded(context, clipData);
                    return result;
                });
                hookedCount++;
            }
            Log.i(TAG, "ClipboardService hooks installed: " + hookedCount);
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to hook ClipboardService", throwable);
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

    private static void sendTextIfNeeded(Context context, ClipData clipData) {
        if (context == null || clipData == null || clipData.getItemCount() == 0) return;
        CharSequence text = extractPlainText(context, clipData);
        if (text == null) return;

        String value = text.toString().trim();
        if (value.isEmpty() || value.length() > Config.CLIPBOARD_TEXT_MAX_LENGTH) return;

        long now = System.currentTimeMillis();
        if (value.equals(lastText) && now - lastSentAt < DUPLICATE_WINDOW_MILLIS) return;
        lastText = value;
        lastSentAt = now;

        Intent intent = new Intent(Config.ACTION_HANDLE_CLIPBOARD_TEXT)
            .setComponent(new ComponentName(Config.APPLICATION_ID, RECEIVER_CLASS))
            .putExtra(Config.EXTRA_CLIPBOARD_TEXT, value)
            .putExtra(Config.EXTRA_CLIPBOARD_SOURCE, "lsposed")
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
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
}
