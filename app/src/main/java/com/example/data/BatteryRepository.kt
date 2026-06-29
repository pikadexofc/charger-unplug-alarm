package com.example.data

import kotlinx.coroutines.flow.Flow

class BatteryRepository(private val dao: BatteryHistoryDao) {
    val history: Flow<List<BatteryHistoryEntity>> = dao.getRecentHistory()

    suspend fun insert(percentage: Int, status: String) {
        dao.insert(BatteryHistoryEntity(percentage = percentage, status = status))
    }
}
