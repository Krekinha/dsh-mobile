package com.dsh.app

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getServerUrl(): String {
        val stored = prefs.getString(KEY_SERVER_URL, null)
        return if (!stored.isNullOrBlank()) {
            UrlHelper.normalize(stored)
        } else {
            UrlHelper.DEFAULT_URL
        }
    }

    fun setServerUrl(url: String) {
        val normalized = UrlHelper.normalize(url)
        prefs.edit().putString(KEY_SERVER_URL, normalized).apply()
    }

    fun resetToDefault(): String {
        prefs.edit().remove(KEY_SERVER_URL).apply()
        return UrlHelper.DEFAULT_URL
    }

    companion object {
        private const val PREFS_NAME = "dsh_preferences"
        private const val KEY_SERVER_URL = "key_server_url"
    }
}
