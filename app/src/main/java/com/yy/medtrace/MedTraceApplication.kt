package com.yy.medtrace

import android.app.Application
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.yy.medtrace.data.AppDatabase

class MedTraceApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        UMConfigure.setLogEnabled(true)
        UMConfigure.preInit(this, UMENG_APPKEY, UMENG_CHANNEL)
        Thread {
            UMConfigure.init(
                this,
                UMENG_APPKEY,
                UMENG_CHANNEL,
                UMConfigure.DEVICE_TYPE_PHONE,
                null
            )
            MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)
        }.start()
    }

    companion object {
        private const val UMENG_APPKEY = "6a5b46a5cbfa6959517c8588"
        private const val UMENG_CHANNEL = "official"
    }
}
