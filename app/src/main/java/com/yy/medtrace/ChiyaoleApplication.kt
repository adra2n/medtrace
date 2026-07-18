package com.yy.medtrace

import android.app.Application
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.util.initUmengIfAllowed

class ChiyaoleApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        // 若用户此前已同意隐私政策，冷启动时即初始化友盟统计
        initUmengIfAllowed(this)
    }
}
