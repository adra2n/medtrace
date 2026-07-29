package com.yy.medtrace

import android.app.Application
import android.content.Context
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.yy.medtrace.common.Constants
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MedTraceApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        UMConfigure.setLogEnabled(BuildConfig.DEBUG)
    }

    fun initAnalytics() {
        val app = this
        UMConfigure.preInit(app, Constants.UMENG_APPKEY, Constants.UMENG_CHANNEL)
        UMConfigure.submitPolicyGrantResult(app, true)
        appScope.launch {
            UMConfigure.init(
                app,
                Constants.UMENG_APPKEY,
                Constants.UMENG_CHANNEL,
                UMConfigure.DEVICE_TYPE_PHONE,
                null
            )
            MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)
        }
    }

    companion object {
        fun initAnalytics(context: Context) {
            (context.applicationContext as? MedTraceApplication)?.initAnalytics()
        }
    }
}
