package it.orma.cammino.core.database.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Preferenze leggere dell'app, persistite con DataStore. */
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reduceAnimations: Boolean = false,
    val audioEnabled: Boolean = false,
    val lastSeenContentVersion: Int = 0,
)

private val Context.settingsDataStore by preferencesDataStore(name = "cammino_settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            onboardingCompleted = prefs[KEY_ONBOARDING] ?: false,
            themeMode = prefs[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            reduceAnimations = prefs[KEY_REDUCE_ANIMATIONS] ?: false,
            audioEnabled = prefs[KEY_AUDIO] ?: false,
            lastSeenContentVersion = prefs[KEY_CONTENT_VERSION] ?: 0,
        )
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_ONBOARDING] = value }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setReduceAnimations(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_REDUCE_ANIMATIONS] = value }
    }

    suspend fun setAudioEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_AUDIO] = value }
    }

    suspend fun setLastSeenContentVersion(value: Int) {
        context.settingsDataStore.edit { it[KEY_CONTENT_VERSION] = value }
    }

    private companion object {
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val KEY_AUDIO = booleanPreferencesKey("audio_enabled")
        val KEY_CONTENT_VERSION = intPreferencesKey("last_seen_content_version")
    }
}
