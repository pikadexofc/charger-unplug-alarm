package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryHistoryDao {
    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<BatteryHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: BatteryHistoryEntity)

    @Query("DELETE FROM battery_history")
    suspend fun clearHistory()
}
