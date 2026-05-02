package com.scheduler.calls.sync

import android.content.Context

class SyncSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim()).apply()

    var lastSyncMs: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    var lastSyncStatus: String
        get() = prefs.getString(KEY_LAST_STATUS, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_STATUS, value).apply()

    val isConfigured: Boolean
        get() = serverUrl.isNotBlank()

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_LAST_SYNC = "last_sync_ms"
        private const val KEY_LAST_STATUS = "last_sync_status"
    }
}
