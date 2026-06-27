package com.mogalab.fliplock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_stats")

data class FocusStats(
    val successfulMinutes: Long = 0L,
    val failedMinutes: Long = 0L,
    val sessionCount: Int = 0,
    val successCount: Int = 0
)

class FocusPreferencesRepository(private val context: Context) {

    private companion object {
        val SUCCESSFUL_MINUTES = longPreferencesKey("successful_minutes")
        val FAILED_MINUTES = longPreferencesKey("failed_minutes")
        val SESSION_COUNT = intPreferencesKey("session_count")
        val SUCCESS_COUNT = intPreferencesKey("success_count")
        val SENSOR_TEST_COMPLETED = booleanPreferencesKey("sensor_test_completed")
    }

    val isSensorTestCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SENSOR_TEST_COMPLETED] ?: false
    }

    suspend fun markSensorTestCompleted() {
        context.dataStore.edit { prefs ->
            prefs[SENSOR_TEST_COMPLETED] = true
        }
    }

    val focusStats: Flow<FocusStats> = context.dataStore.data.map { prefs ->
        FocusStats(
            successfulMinutes = prefs[SUCCESSFUL_MINUTES] ?: 0L,
            failedMinutes = prefs[FAILED_MINUTES] ?: 0L,
            sessionCount = prefs[SESSION_COUNT] ?: 0,
            successCount = prefs[SUCCESS_COUNT] ?: 0
        )
    }

    suspend fun recordSuccess(durationMinutes: Long) {
        context.dataStore.edit { prefs ->
            prefs[SUCCESSFUL_MINUTES] = (prefs[SUCCESSFUL_MINUTES] ?: 0L) + durationMinutes
            prefs[SESSION_COUNT] = (prefs[SESSION_COUNT] ?: 0) + 1
            prefs[SUCCESS_COUNT] = (prefs[SUCCESS_COUNT] ?: 0) + 1
        }
    }

    suspend fun recordFailure(focusedMinutes: Long) {
        context.dataStore.edit { prefs ->
            prefs[FAILED_MINUTES] = (prefs[FAILED_MINUTES] ?: 0L) + focusedMinutes
            prefs[SESSION_COUNT] = (prefs[SESSION_COUNT] ?: 0) + 1
        }
    }
}
