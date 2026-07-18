package com.yy.medtrace

import android.app.Application
import com.yy.medtrace.data.AppDatabase

class MedTraceApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
