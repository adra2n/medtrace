package com.yy.chiyaole

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.yy.chiyaole.data.AppDatabase

class ChiyaoleApplication : Application(), Configuration.Provider {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        
        // 初始化 WorkManager
        WorkManager.initialize(
            this,
            workManagerConfiguration
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
