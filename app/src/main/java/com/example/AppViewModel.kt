package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BatteryHistoryEntity
import com.example.data.BatteryRepository
import com.example.data.DataStoreManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val dataStoreManager: DataStoreManager,
    private val batteryRepository: BatteryRepository
) : ViewModel() {

    val isFirstLaunch: StateFlow<Boolean> = dataStoreManager.isFirstLaunch.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )
    val targetPercentage: StateFlow<Int> = dataStoreManager.targetPercentage.stateIn(
        viewModelScope, SharingStarted.Eagerly, 80
    )
    val isProtectionEnabled: StateFlow<Boolean> = dataStoreManager.isProtectionEnabled.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )
    val isAlarmRinging: StateFlow<Boolean> = dataStoreManager.isAlarmRinging.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )
    val autoStartOnReboot: StateFlow<Boolean> = dataStoreManager.autoStartOnReboot.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )
    val vibrationEnabled: StateFlow<Boolean> = dataStoreManager.vibrationEnabled.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )
    val passwordHash: StateFlow<String?> = dataStoreManager.passwordHash.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )
    val securityQuestionId: StateFlow<Int> = dataStoreManager.securityQuestionId.stateIn(
        viewModelScope, SharingStarted.Eagerly, 0
    )
    val securityAnswerHash: StateFlow<String?> = dataStoreManager.securityAnswerHash.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )
    val alarmSound: StateFlow<Int> = dataStoreManager.alarmSound.stateIn(
        viewModelScope, SharingStarted.Eagerly, 0
    )

    val batteryHistory: StateFlow<List<BatteryHistoryEntity>> = batteryRepository.history.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun completeFirstLaunch() {
        viewModelScope.launch { dataStoreManager.setFirstLaunchCompleted() }
    }

    fun setTargetPercentage(percentage: Int) {
        viewModelScope.launch { dataStoreManager.setTargetPercentage(percentage) }
    }

    fun setProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setProtectionEnabled(enabled) }
    }

    fun setAlarmRinging(ringing: Boolean) {
        viewModelScope.launch { dataStoreManager.setAlarmRinging(ringing) }
    }

    fun setPassword(hash: String) {
        viewModelScope.launch { dataStoreManager.setPasswordHash(hash) }
    }

    fun setSecurityData(questionId: Int, answerHash: String) {
        viewModelScope.launch { dataStoreManager.setSecurityData(questionId, answerHash) }
    }

    fun setAutoStart(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setAutoStartOnReboot(enabled) }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setVibrationEnabled(enabled) }
    }

    fun setAlarmSound(soundId: Int) {
        viewModelScope.launch { dataStoreManager.setAlarmSound(soundId) }
    }

    class Factory(
        private val dataStoreManager: DataStoreManager,
        private val batteryRepository: BatteryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(dataStoreManager, batteryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
