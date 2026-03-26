package com.autotypehid.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val SELECTED_PROFILE = stringPreferencesKey("selected_profile")
        private val TYPING_SPEED_MULTIPLIER = floatPreferencesKey("typing_speed_multiplier")
        private val TYPO_PROBABILITY_OVERRIDE = floatPreferencesKey("typo_probability_override")
        private val WORD_GAP_MS = intPreferencesKey("word_gap_ms")
        private val JITTER_PERCENT = intPreferencesKey("jitter_percent")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val selectedProfile: Flow<String> = context.settingsDataStore.data.map { pref ->
        pref[SELECTED_PROFILE] ?: "NORMAL"
    }

    val typingSpeedMultiplier: Flow<Float> = context.settingsDataStore.data.map { pref ->
        pref[TYPING_SPEED_MULTIPLIER] ?: 1.0f
    }

    val typoProbabilityOverride: Flow<Float> = context.settingsDataStore.data.map { pref ->
        pref[TYPO_PROBABILITY_OVERRIDE] ?: -1f
    }

    val wordGapMs: Flow<Int> = context.settingsDataStore.data.map { pref ->
        pref[WORD_GAP_MS] ?: 80
    }

    val jitterPercent: Flow<Int> = context.settingsDataStore.data.map { pref ->
        pref[JITTER_PERCENT] ?: 18
    }

    val themeMode: Flow<String> = context.settingsDataStore.data.map { pref ->
        pref[THEME_MODE] ?: "SYSTEM"
    }

    suspend fun saveProfile(profile: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[SELECTED_PROFILE] = profile
        }
    }

    suspend fun saveTypingSpeed(value: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[TYPING_SPEED_MULTIPLIER] = value
        }
    }

    suspend fun saveTypoProbability(value: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[TYPO_PROBABILITY_OVERRIDE] = value
        }
    }

    suspend fun saveWordGapMs(value: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[WORD_GAP_MS] = value
        }
    }

    suspend fun saveJitterPercent(value: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[JITTER_PERCENT] = value
        }
    }

    suspend fun saveThemeMode(value: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[THEME_MODE] = value
        }
    }
}
