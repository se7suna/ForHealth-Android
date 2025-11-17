package com.example.forhealth.utils

import android.content.Context
import android.content.SharedPreferences

object PrefsHelper {

    private const val PREFS_NAME = "forhealth_prefs"
    private const val KEY_TOKEN = "key_token"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(context: Context, token: String) {
        val editor = getPrefs(context).edit()
        editor.putString(KEY_TOKEN, token)
        editor.apply()
    }

    fun getToken(context: Context): String {
        return getPrefs(context).getString(KEY_TOKEN, "") ?: ""
    }

    fun clearToken(context: Context) {
        val editor = getPrefs(context).edit()
        editor.remove(KEY_TOKEN)
        editor.apply()
    }
}
