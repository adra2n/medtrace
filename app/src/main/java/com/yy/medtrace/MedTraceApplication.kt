package com.yy.medtrace

import android.app.Application
import android.content.Context
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.yy.medtrace.data.AppDatabase

class MedTraceApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        UMConfigure.setLogEnabled(true)
    }

    /**
     * 在用户同意隐私政策后调用：完成友盟 SDK 初始化与数据上报。
     * 暨 preInit（轻量预初始化，不采集数据）+ init（正式初始化）。
     */
    fun initAnalytics() {
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

        fun initAnalytics(context: Context) {
            (context.applicationContext as? MedTraceApplication)?.initAnalytics()
        }
    }
}
