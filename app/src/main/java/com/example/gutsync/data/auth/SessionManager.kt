package com.example.gutsync.data.auth

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun getSession(): AuthSession {
        val raw = prefs.getString(KEY_SESSION, null) ?: return AuthSession()
        return try {
            json.decodeFromString<AuthSession>(raw)
        } catch (e: Exception) {
            AuthSession()
        }
    }

    fun saveSession(session: AuthSession) {
        prefs.edit().putString(KEY_SESSION, json.encodeToString(session)).apply()
    }

    fun isLoggedIn(): Boolean = getSession().isLoggedIn

    fun clearSession() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    companion object {
        private const val PREFS_NAME = "gutsync_auth"
        private const val KEY_SESSION = "session"
    }
}
