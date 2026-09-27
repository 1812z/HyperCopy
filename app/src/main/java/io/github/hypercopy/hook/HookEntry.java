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
import android.app.Application;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import io.github.hypercopy.Config;
import io.github.libxposed.api.XposedModule;

public class HookEntry extends XposedModule {
    private static final String TAG = "HyperCopy";
    private static final String CLIPBOARD_SERVICE_CLASS = "com.android.server.clipboard.ClipboardService";
    private static final String RECEIVER_CLASS = "io.github.hypercopy.clipboard.handling.ClipboardTextReceiver";
    private static final String SERVICE_CLASS = "io.github.hypercopy.clipboard.handling.ClipboardTextService";
    private static final long DUPLICATE_WINDOW_MILLIS = 1500L;
    private static final int INSTALL_RETRY_LIMIT = 20;
    private static final long INSTALL_RETRY_DELAY_MILLIS = 1000L;
    private static final int CLEAR_RECEIVER_RETRY_LIMIT = 30;
    private static final long CLEAR_RECEIVER_RETRY_DELAY_MILLIS = 1000L;
    private static final String CONTENT_EXTENSION_ACTION = "miui.intent.action.TEXT_CONTENT_EXTENSION";
    private static final String CONTENT_EXTENSION_PACKAGE = "com.miui.contentextension";
    private static final String CONTENT_EXTENSION_EXTRA = "clipboard_data";
    private static final String AICR_PACKAGE = "com.xiaomi.aicr";

    private static String lastText = "";
    private static long lastSentAt = 0L;
    private static String lastSystemCopyText = "";
    private static long lastSystemCopyAt = 0L;
    private boolean hooksInstalled = false;
    private boolean aicrAttachHookInstalled = false;
    private boolean aicrClipboardHookInstalled = false;
    private Context aicrContext;
    private boolean clearReceiverRegistered = false;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        HookLog.init(this);
        logDebug("module loaded: process=" + param.getProcessName()
            + ", systemServer=" + param.isSystemServer()
            + ", api=" + getApiVersion());
    }

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        installClipboardHooksWithRetry(param.getClassLoader(), "onSystemServerStarting", 0);
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        if (!AICR_PACKAGE.equals(param.getPackageName()) || !param.isFirstPackage()) return;
        installAicrAttachHook();
    }

    private void installAicrAttachHook() {
        if (aicrAttachHookInstalled) return;
        try {
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            attach.setAccessible(true);
            hook(attach).setId("hypercopy_aicr_attach").intercept(chain -> {
                Object context = chain.getArg(0);
                Object result = chain.proceed();
                if (context instanceof Context) {
                    Context applicationContext = ((Context) context).getApplicationContext();
                    aicrContext = applicationContext != null ? applicationContext : (Context) context;
                }
                installAicrClipboardHook();
                return result;
            });
            aicrAttachHookInstalled = true;
            logDebug("AICR Application.attach hook installed");
        } catch (Throwable throwable) {
            logError("Failed to hook AICR Application.attach", throwable);
        }
    }

    private void installAicrClipboardHook() {
        if (aicrClipboardHookInstalled) return;
        try {
            Method getPrimaryClip = ClipboardManager.class.getDeclaredMethod("getPrimaryClip");
            getPrimaryClip.setAccessible(true);
            hook(getPrimaryClip).setId("hypercopy_aicr_get_primary_clip").intercept(chain -> {
                Object result = chain.proceed();
                if (result instanceof ClipData && shouldUseCustomJumpMode()
                    && handleClipboardThroughApp((ClipData) result)) {
                    logDebug("blocked matched clipboard from AICR");
                    return ClipData.newPlainText("", "");
                }
                return result;
            });
            aicrClipboardHookInstalled = true;
            logDebug("AICR ClipboardManager.getPrimaryClip hook installed");
        } catch (Throwable throwable) {
            logError("Failed to hook AICR ClipboardManager.getPrimaryClip", throwable);
        }
    }

    private boolean handleClipboardThroughApp(ClipData clipData) {
        Context context = aicrContext;
        if (context == null || clipData.getItemCount() == 0) return false;
        CharSequence text = clipData.getItemAt(0).getText();
        if (text == null) return false;
        String value = text.toString().trim();
        if (value.isEmpty() || value.length() > Config.CLIPBOARD_TEXT_MAX_LENGTH) return false;
        try {
            android.os.Bundle result = context.getContentResolver().call(
                Uri.parse("content://" + Config.CLIPBOARD_MATCH_PROVIDER_AUTHORITY),
                Config.CLIPBOARD_MATCH_PROVIDER_METHOD,
                value,
                null
            );
            return result != null && result.getBoolean(Config.CLIPBOARD_MATCH_PROVIDER_RESULT, false);
        } catch (Throwable throwable) {
            logWarn("handle clipboard through app failed", throwable);
            return false;
        }
    }

    private boolean shouldUseCustomJumpMode() {
        try {
            android.content.SharedPreferences preferences = getRemotePreferences(Config.PREFS_NAME);
            String mode = preferences.getString(
                Config.KEY_LSPOSED_JUMP_NOTIFICATION_MODE,
                preferences.getString(Config.KEY_JUMP_NOTIFICATION_MODE, Config.DEFAULT_JUMP_NOTIFICATION_MODE)
            );
            return Config.JUMP_NOTIFICATION_MODE_MIUI_ISLAND.equals(mode)
                || Config.JUMP_NOTIFICATION_MODE_SYSTEM_COPY.equals(mode);
        } catch (Throwable ignored) {
            return false;
        }
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
            Set<Method> hookedMethods = new HashSet<>();
            int hookedCount = hookClipboardMethods(clipboardServiceClass, hookedMethods);
            for (Class<?> declaredClass : clipboardServiceClass.getDeclaredClasses()) {
                hookedCount += hookClipboardMethods(declaredClass, hookedMethods);
            }
            hooksInstalled = hookedCount > 0;
            if (hooksInstalled) {
                registerClearReceiverWithRetry(0);
            }
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

    private int hookClipboardMethods(Class<?> targetClass, Set<Method> hookedMethods) {
        int hookedCount = 0;
        for (Method method : targetClass.getDeclaredMethods()) {
            if (!isSetPrimaryClipMethod(method) || !hookedMethods.add(method)) continue;
            method.setAccessible(true);
            logDebug("hook ClipboardService method: " + method.toGenericString());
            hook(method).setId("hypercopy_clipboard_" + method.toGenericString()).intercept(chain -> {
                int callingUid = Binder.getCallingUid();
            Object[] args = chain.getArgs().toArray();
            ClipData originalClipData = findClipData(args);
            boolean systemCopy = originalClipData != null && isSystemCopyClip(originalClipData);
            boolean suppressNativeBar = shouldSuppressNativeBar(originalClipData);
            Object[] callArgs = suppressNativeBar ? suppressNativeBar(args) : args;
            if (suppressNativeBar && originalClipData != null) {
                logDebug("suppressed native ContentExtension bar, label="
                    + String.valueOf(originalClipData.getDescription().getLabel()));
            }
            Object result = chain.proceed(callArgs);
            try {
                ClipData clipData = findClipData(callArgs);
                Context context = findContext(chain.getThisObject());
                if (context == null) context = findSystemContext();
                if (systemCopy && shouldStartSystemCopy(originalClipData)) {
                    startMiuiContentExtension(context, originalClipData);
                }
                if (!systemCopy) {
                    sendTextIfNeeded(context, clipData, callArgs, callingUid);
                }
                } catch (Throwable throwable) {
                    logWarn("clipboard hook callback failed", throwable);
                }
                return result;
            });
            hookedCount++;
        }
        return hookedCount;
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

    private static Object[] suppressNativeBar(Object[] args) {
        for (int index = 0; index < args.length; index++) {
            if (!(args[index] instanceof ClipData)) continue;
            ClipData original = (ClipData) args[index];
            if (original.getItemCount() == 0) continue;
            ClipDescription description = original.getDescription();
            String[] mimeTypes = new String[description.getMimeTypeCount()];
            for (int mimeIndex = 0; mimeIndex < mimeTypes.length; mimeIndex++) {
                mimeTypes[mimeIndex] = description.getMimeType(mimeIndex);
            }
            ClipData replacement = new ClipData(
                "universalClipData",
                mimeTypes,
                original.getItemAt(0)
            );
            for (int itemIndex = 1; itemIndex < original.getItemCount(); itemIndex++) {
                replacement.addItem(original.getItemAt(itemIndex));
            }
            args[index] = replacement;
            break;
        }
        return args;
    }

    private static boolean isSystemCopyClip(ClipData clipData) {
        CharSequence label = clipData.getDescription().getLabel();
        return label != null && "HyperCopySystemCopy".contentEquals(label);
    }

    private boolean shouldSuppressNativeBar(ClipData clipData) {
        if (clipData == null || clipData.getItemCount() == 0) return false;
        CharSequence text = clipData.getItemAt(0).getText();
        if (text == null || text.toString().trim().isEmpty()) return false;
        if (!shouldUseCustomJumpMode()) return false;
        try {
            android.content.SharedPreferences preferences = getRemotePreferences(Config.PREFS_NAME);
            Set<String> patterns = preferences.getStringSet(Config.KEY_LSPOSED_MATCH_PATTERNS, null);
            if (patterns == null || patterns.isEmpty()) return false;
            String value = text.toString().trim();
            for (String pattern : patterns) {
                try {
                    if (Pattern.compile(pattern).matcher(value).find()) return true;
                } catch (PatternSyntaxException ignored) {
                }
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void startMiuiContentExtension(Context context, ClipData clipData) {
        if (context == null || clipData == null || clipData.getItemCount() == 0) return;
        CharSequence text = clipData.getItemAt(0).getText();
        if (text == null || text.toString().trim().isEmpty()) return;
        Intent serviceIntent = new Intent(CONTENT_EXTENSION_ACTION)
            .setPackage(CONTENT_EXTENSION_PACKAGE)
            .putExtra(CONTENT_EXTENSION_EXTRA, text.toString().trim());
        long identity = Binder.clearCallingIdentity();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "started MIUI ContentExtension for system copy, text=" + text.toString().trim());
        } catch (Throwable throwable) {
            Log.w(TAG, "start MIUI ContentExtension failed", throwable);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static synchronized boolean shouldStartSystemCopy(ClipData clipData) {
        if (clipData == null || clipData.getItemCount() == 0 || clipData.getItemAt(0).getText() == null) return false;
        String text = clipData.getItemAt(0).getText().toString().trim();
        long now = System.currentTimeMillis();
        if (text.equals(lastSystemCopyText) && now - lastSystemCopyAt < 1_500L) return false;
        lastSystemCopyText = text;
        lastSystemCopyAt = now;
        return true;
    }

    private static Context findContext(Object service) {
        return findContext(service, new HashSet<>(), 0);
    }

    private static Context findContext(Object service, Set<Object> visited, int depth) {
        if (service == null || depth > 2 || !visited.add(service)) return null;
        Class<?> current = service.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(service);
                    if (value instanceof Context) return (Context) value;
                    if (field.isSynthetic() || field.getName().startsWith("this$")) {
                        Context context = findContext(value, visited, depth + 1);
                        if (context != null) return context;
                    }
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

    private void sendTextIfNeeded(Context context, ClipData clipData, Object[] args, int callingUid) {
        if (context == null || clipData == null || clipData.getItemCount() == 0) return;
        CharSequence text = extractPlainText(context, clipData);
        if (text == null) return;

        String value = text.toString().trim();
        if (value.isEmpty() || value.length() > Config.CLIPBOARD_TEXT_MAX_LENGTH) return;

        long now = System.currentTimeMillis();
        if (value.equals(lastText) && now - lastSentAt < DUPLICATE_WINDOW_MILLIS) return;
        lastText = value;
        lastSentAt = now;
        String sourcePackage = findSourcePackage(context, args, callingUid);
        int sourceUserId = callingUid / 100_000;

        Intent intent = new Intent(Config.ACTION_HANDLE_CLIPBOARD_TEXT)
            .setComponent(new ComponentName(Config.APPLICATION_ID, SERVICE_CLASS))
            .putExtra(Config.EXTRA_CLIPBOARD_TEXT, value)
            .putExtra(Config.EXTRA_CLIPBOARD_SOURCE, sourcePackage);
        logDebug("send clipboard text to app, length=" + value.length() + ", source=" + sourcePackage
            + ", uid=" + callingUid + ", user=" + sourceUserId);
        long identity = Binder.clearCallingIdentity();
        try {
            context.startService(intent);
        } catch (Throwable throwable) {
            logWarn("start clipboard handling service failed, falling back to broadcast", throwable);
            Intent fallback = new Intent(intent)
                .setComponent(new ComponentName(Config.APPLICATION_ID, RECEIVER_CLASS))
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcastAsUser(fallback, android.os.Process.myUserHandle());
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static String findSourcePackage(Context context, Object[] args, int callingUid) {
        String[] callingPackages = context.getPackageManager().getPackagesForUid(callingUid);
        if (callingPackages != null && callingPackages.length > 0) return callingPackages[0];
        if (args == null) return "";
        for (Object arg : args) {
            if (!(arg instanceof String)) continue;
            String value = ((String) arg).trim();
            if (looksLikePackageName(value)) return value;
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

    private static boolean looksLikePackageName(String value) {
        return !value.isEmpty() && value.contains(".") && !value.contains(" ");
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
        HookLog.d(this, TAG, message);
    }

    private void logWarn(String message, Throwable throwable) {
        HookLog.w(this, TAG, message, throwable);
    }

    private void logError(String message, Throwable throwable) {
        HookLog.e(this, TAG, message, throwable);
    }
}
