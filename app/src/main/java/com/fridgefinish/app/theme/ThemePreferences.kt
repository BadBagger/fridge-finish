package com.fridgefinish.app.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fridgeFinishThemeDataStore by preferencesDataStore(name = "fridge_finish_theme")

object ThemePreferences {
    private val selectedThemeStyle = stringPreferencesKey("selectedThemeStyle")
    private val selectedAppearanceMode = stringPreferencesKey("selectedAppearanceMode")

    fun selectedThemeStyle(context: Context): Flow<AppThemeStyle> =
        context.fridgeFinishThemeDataStore.data.map { preferences ->
            preferences[selectedThemeStyle]
                ?.let { runCatching { AppThemeStyle.valueOf(it) }.getOrNull() }
                ?: AppThemeStyle.OriginalFresh
        }

    fun selectedAppearanceMode(context: Context): Flow<AppearanceMode> =
        context.fridgeFinishThemeDataStore.data.map { preferences ->
            preferences[selectedAppearanceMode]
                ?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() }
                ?: AppearanceMode.FollowSystem
        }

    suspend fun setThemeStyle(context: Context, style: AppThemeStyle) {
        context.fridgeFinishThemeDataStore.edit { preferences ->
            preferences[selectedThemeStyle] = style.name
        }
    }

    suspend fun setAppearanceMode(context: Context, mode: AppearanceMode) {
        context.fridgeFinishThemeDataStore.edit { preferences ->
            preferences[selectedAppearanceMode] = mode.name
        }
    }
}
