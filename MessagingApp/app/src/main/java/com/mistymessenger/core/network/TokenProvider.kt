package com.mistymessenger.core.network

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("auth_prefs")

@Singleton
class TokenProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
    }

    fun getAccessToken(): String = runBlocking {
        context.dataStore.data.map { it[ACCESS_TOKEN] ?: "" }.first()
    }

    fun getUserId(): String = runBlocking {
        context.dataStore.data.map { it[USER_ID] ?: "" }.first()
    }

    suspend fun save(accessToken: String, refreshToken: String, userId: String) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(ACCESS_TOKEN, accessToken)
                set(REFRESH_TOKEN, refreshToken)
                set(USER_ID, userId)
            }
        }
    }

    suspend fun clear() {
        context.dataStore.updateData { it.toMutablePreferences().apply { clear() } }
    }
}
