package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_history")
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val percentage: Int,
    val status: String // "CHARGING", "DISCHARGED", "FULL", "ALARM_TRIGGERED"
)
