package com.yy.medtrace.util

import android.content.Context
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.yy.medtrace.BuildConfig
import com.yy.medtrace.data.settings.PrivacyStore

/**
 * 友盟 U-App 统计初始化。
 * 合规要求：必须在用户同意隐私政策后调用。
 * 无 AppKey 时跳过（local.properties 未配置 UMENG_APPKEY），不影响主流程。
 */
fun initUmengIfAllowed(context: Context) {
    val key = BuildConfig.UMENG_APPKEY
    if (key.isBlank()) return
    if (!PrivacyStore(context).isAgreedSync()) return

    UMConfigure.init(
        context.applicationContext,
        key,
        "Umeng",
        UMConfigure.DEVICE_TYPE_PHONE,
        null
    )
    // 自动页面统计：在各 Screen 进入/退出时由框架采集，无需手动埋点
    MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)
}
