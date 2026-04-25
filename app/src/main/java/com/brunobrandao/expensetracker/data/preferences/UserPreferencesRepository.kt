package com.brunobrandao.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val context: Context
) {

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
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
}
