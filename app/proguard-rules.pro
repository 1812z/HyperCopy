# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Loaded by libxposed from META-INF/xposed/java_init.list. The class name must
# remain stable even when release builds are obfuscated.
-keep class io.github.hypercopy.hook.HookEntry { *; }

# Keep Android component class names because some of them are launched by
# explicit class names from hooks, shell commands, or manifest/provider glue.
-keep class io.github.hypercopy.App { *; }
-keep class io.github.hypercopy.ui.** extends android.app.Activity { *; }
-keep class io.github.hypercopy.clipboard.** extends android.app.Activity { *; }
-keep class io.github.hypercopy.clipboard.** extends android.content.BroadcastReceiver { *; }

# Started by Shizuku app_process using its literal class name. Release builds
# must keep both the class name and main(String[]) entry point.
-keep class io.github.hypercopy.clipboard.MiuiXmsfFirewallBinderCommand { *; }

# Shizuku and libxposed use binder/service integration and generated provider
# metadata. Preserve public API shape while still allowing unused code removal.
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keep class io.github.libxposed.** { *; }
