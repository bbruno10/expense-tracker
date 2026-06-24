package com.brunobrandao.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val userPreferences: Flow<UserPreferences> get() = context.dataStore.data.map { preferences ->
        val currency = preferences[Keys.CURRENCY]?.let { code ->
            Currency.entries.find { it.code == code }
        } ?: Currency.USD

        val themeMode = preferences[Keys.THEME_MODE]?.let { name ->
            try { ThemeMode.valueOf(name) } catch (_: Exception) { ThemeMode.SYSTEM }
        } ?: ThemeMode.SYSTEM

        UserPreferences(
            currency = currency,
            themeMode = themeMode
        )
    }

    suspend fun updateCurrency(currency: Currency) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CURRENCY] = currency.code
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }

    /** Writes currency code to Firestore. Best-effort: network failures are swallowed. */
    suspend fun pushCurrency(userId: String, currency: Currency) {
        try {
            settingsRef(userId)
                .set(mapOf("currency" to currency.code), SetOptions.merge())
                .await()
        } catch (_: Exception) {}
    }

    /** One-time read of the stored currency code. Returns null if absent or on any error. */
    suspend fun pullCurrency(userId: String): String? {
        return try {
            settingsRef(userId).get().await().getString("currency")
        } catch (_: Exception) {
            null
        }
    }

    /** Removes the currency key from DataStore so the next read falls back to the USD default. */
    suspend fun clearCurrency() {
        context.dataStore.edit { it.remove(Keys.CURRENCY) }
    }

    /** Removes all user preference keys. Used on account deletion. */
    suspend fun clearAllPreferences() {
        context.dataStore.edit { it.clear() }
    }

    private fun settingsRef(userId: String) = firestore
        .collection("users").document(userId)
        .collection("settings").document("preferences")
}
