package io.github.hypercopy.clipboard.handling

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import io.github.hypercopy.Config

class ClipboardMatchProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != Config.CLIPBOARD_MATCH_PROVIDER_METHOD) return super.call(method, arg, extras)
        val appContext = context?.applicationContext ?: return result(false)
        val callingUid = Binder.getCallingUid()
        val packages = appContext.packageManager.getPackagesForUid(callingUid).orEmpty()
        if (callingUid != Process.SYSTEM_UID && AICR_PACKAGE !in packages) return result(false)
        return result(ClipboardTextHandler.handle(appContext, arg.orEmpty(), ""))
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private fun result(handled: Boolean) = Bundle().apply {
        putBoolean(Config.CLIPBOARD_MATCH_PROVIDER_RESULT, handled)
    }

    private companion object {
        const val AICR_PACKAGE = "com.xiaomi.aicr"
    }
}
