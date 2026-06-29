package com.example

import android.content.Context
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.BatteryRepository
import com.example.data.DataStoreManager

class AppContainer(private val context: Context) {
    val dataStoreManager by lazy {
        DataStoreManager(context)
    }

    private val database by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "charge_guardian_db")
            .build()
    }

    val batteryRepository by lazy {
        BatteryRepository(database.batteryHistoryDao())
    }
}

class ChargeGuardianApp : android.app.Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
