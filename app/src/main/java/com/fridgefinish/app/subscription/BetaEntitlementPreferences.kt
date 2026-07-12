package com.fridgefinish.app.subscription

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.betaEntitlementDataStore by preferencesDataStore(name = "fridge_finish_beta_entitlement")

object BetaEntitlementPreferences {
    private val betaPremiumEnabled = booleanPreferencesKey("betaPremiumEnabled")

    fun enabled(context: Context): Flow<Boolean> =
        context.betaEntitlementDataStore.data.map { preferences ->
            preferences[betaPremiumEnabled] ?: false
        }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.betaEntitlementDataStore.edit { preferences ->
            preferences[betaPremiumEnabled] = enabled
        }
    }
}
