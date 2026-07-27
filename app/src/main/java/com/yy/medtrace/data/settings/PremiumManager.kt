package com.yy.medtrace.data.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 高级功能管理器
 * 管理应用内购买状态和功能解锁
 */
@Singleton
class PremiumManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "premium_prefs", Context.MODE_PRIVATE
    )

    companion object {
        // 产品 ID
        const val PRODUCT_UNLOCK_ALL = "medtrace_unlock_all"
        
        // 本地存储 key
        private const val KEY_PREMIUM_ACTIVE = "premium_active"
        private const val KEY_PURCHASE_TIME = "purchase_time"
        private const val KEY_ORDER_ID = "order_id"
        
        // 功能限制
        const val FREE_MEMBER_LIMIT = 1
        const val FREE_AI_DAILY_LIMIT = 3
    }

    /**
     * 是否已解锁高级功能
     */
    fun isPremiumActive(): Boolean {
        return prefs.getBoolean(KEY_PREMIUM_ACTIVE, false)
    }

    /**
     * 获取购买时间
     */
    fun getPurchaseTime(): Long {
        return prefs.getLong(KEY_PURCHASE_TIME, 0)
    }

    /**
     * 获取订单号
     */
    fun getOrderId(): String? {
        return prefs.getString(KEY_ORDER_ID, null)
    }

    /**
     * 保存购买状态（购买成功后调用）
     */
    fun savePurchase(orderId: String) {
        prefs.edit()
            .putBoolean(KEY_PREMIUM_ACTIVE, true)
            .putLong(KEY_PURCHASE_TIME, System.currentTimeMillis())
            .putString(KEY_ORDER_ID, orderId)
            .apply()
    }

    /**
     * 验证购买状态（启动时调用）
     */
    fun verifyPurchase(): Boolean {
        // 本地验证，实际项目中应该调用服务器验证
        return isPremiumActive()
    }

    /**
     * 检查是否可以添加更多成员
     */
    fun canAddMember(currentCount: Int): Boolean {
        if (isPremiumActive()) return true
        return currentCount < FREE_MEMBER_LIMIT
    }

    /**
     * 检查今日 AI 识别次数是否用完
     */
    fun canUseAiToday(todayCount: Int): Boolean {
        if (isPremiumActive()) return true
        return todayCount < FREE_AI_DAILY_LIMIT
    }

    /**
     * 检查是否可以使用某功能
     */
    fun canUseFeature(feature: PremiumFeature): Boolean {
        if (isPremiumActive()) return true
        return feature.isFree
    }

    /**
     * 清除购买状态（用于测试）
     */
    fun clearPurchase() {
        prefs.edit().clear().apply()
    }
}

/**
 * 高级功能枚举
 */
enum class PremiumFeature(val isFree: Boolean, val displayName: String, val description: String) {
    // 免费功能
    BASIC_RECORD(true, "基础记录", "记录就诊信息"),
    SINGLE_MEMBER(true, "单成员", "管理一个家庭成员"),
    BASIC_REMINDER(true, "基本提醒", "设置简单提醒"),
    BASIC_SECURITY(true, "基本安全", "应用锁和 PIN"),
    
    // 付费功能
    UNLIMITED_RECORDS(false, "无限记录", "无限制的就诊记录"),
    UNLIMITED_MEMBERS(false, "多成员", "管理多个家庭成员"),
    ADVANCED_REMINDER(false, "高级提醒", "复杂的重复提醒"),
    DATA_EXPORT(false, "数据导出", "导出数据为文件"),
    DATA_IMPORT(false, "数据导入", "从文件导入数据"),
    ENCRYPTED_BACKUP(false, "加密备份", "AES-256 加密备份"),
    GIST_SYNC(false, "云端同步", "同步到 GitHub Gist"),
    AI_RECOGNITION(false, "AI 识别", "智能识别病历信息"),
    FULL_SECURITY(false, "完整安全", "全部安全功能")
}
