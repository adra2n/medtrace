package com.yy.medtrace

import android.app.Application
import com.yy.medtrace.data.AppDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MedTraceApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
