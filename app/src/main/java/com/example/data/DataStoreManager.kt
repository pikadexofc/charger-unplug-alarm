package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("charge_guardian_prefs")

class DataStoreManager(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val TARGET_PERCENTAGE = intPreferencesKey("target_percentage")
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val SECURITY_QUESTION_ID = intPreferencesKey("security_question_id")
        val SECURITY_ANSWER_HASH = stringPreferencesKey("security_answer_hash")
        val IS_PROTECTION_ENABLED = booleanPreferencesKey("is_protection_enabled")
        val IS_ALARM_RINGING = booleanPreferencesKey("is_alarm_ringing")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val AUTO_START_ON_REBOOT = booleanPreferencesKey("auto_start_on_reboot")
        val ALARM_SOUND = intPreferencesKey("alarm_sound")
    }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { it[IS_FIRST_LAUNCH] ?: true }
    val targetPercentage: Flow<Int> = dataStore.data.map { it[TARGET_PERCENTAGE] ?: 80 }
    val passwordHash: Flow<String?> = dataStore.data.map { it[PASSWORD_HASH] }
    val securityQuestionId: Flow<Int> = dataStore.data.map { it[SECURITY_QUESTION_ID] ?: 0 }
    val securityAnswerHash: Flow<String?> = dataStore.data.map { it[SECURITY_ANSWER_HASH] }
    val isProtectionEnabled: Flow<Boolean> = dataStore.data.map { it[IS_PROTECTION_ENABLED] ?: false }
    val isAlarmRinging: Flow<Boolean> = dataStore.data.map { it[IS_ALARM_RINGING] ?: false }
    val vibrationEnabled: Flow<Boolean> = dataStore.data.map { it[VIBRATION_ENABLED] ?: true }
    val autoStartOnReboot: Flow<Boolean> = dataStore.data.map { it[AUTO_START_ON_REBOOT] ?: true }
    val alarmSound: Flow<Int> = dataStore.data.map { it[ALARM_SOUND] ?: 0 }

    suspend fun setFirstLaunchCompleted() {
        dataStore.edit { it[IS_FIRST_LAUNCH] = false }
    }

    suspend fun setTargetPercentage(percentage: Int) {
        dataStore.edit { it[TARGET_PERCENTAGE] = percentage }
    }

    suspend fun setPasswordHash(hash: String) {
        dataStore.edit { it[PASSWORD_HASH] = hash }
    }

    suspend fun setSecurityData(questionId: Int, answerHash: String) {
        dataStore.edit {
            it[SECURITY_QUESTION_ID] = questionId
            it[SECURITY_ANSWER_HASH] = answerHash
        }
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        dataStore.edit { it[IS_PROTECTION_ENABLED] = enabled }
    }

    suspend fun setAlarmRinging(ringing: Boolean) {
        dataStore.edit { it[IS_ALARM_RINGING] = ringing }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    suspend fun setAutoStartOnReboot(enabled: Boolean) {
        dataStore.edit { it[AUTO_START_ON_REBOOT] = enabled }
    }

    suspend fun setAlarmSound(sound: Int) {
        dataStore.edit { it[ALARM_SOUND] = sound }
    }
}
