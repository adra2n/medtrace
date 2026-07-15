package com.yy.chiyaole

import android.app.Application
import com.yy.chiyaole.data.AppDatabase

class ChiyaoleApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
