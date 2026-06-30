package io.github.hypercopy.hook;

import android.util.Log;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;

public class HookEntry extends XposedModule {
    private static final String TAG = "HyperCopy";

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        Log.i(TAG, "module loaded: process=" + param.getProcessName() + ", api=" + getApiVersion());
    }
}
