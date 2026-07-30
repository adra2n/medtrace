package com.yy.medtrace.data.settings

import com.yy.medtrace.data.RegistrationCodeNative
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 高级功能管理器
 * 通过注册码验证VIP状态
 */
@Singleton
class PremiumManager @Inject constructor(
    private val registrationCodeNative: RegistrationCodeNative
) {
    companion object {
        // 免费用户成员上限
        const val FREE_MEMBER_LIMIT = 2
    }

    /**
     * 是否已解锁高级功能
     * 每次都会通过注册码重新验证
     */
    fun isPremiumActive(): Boolean {
        return registrationCodeNative.isVipActive()
    }

    /**
     * 激活VIP（输入注册码后调用）
     * @return true 如果注册码有效
     */
    fun activateVip(code: String): Boolean {
        val isValid = registrationCodeNative.verifyCode(code)
        if (isValid) {
            registrationCodeNative.saveCode(code)
        }
        return isValid
    }

    /**
     * 获取设备ID（供用户联系开发者时提供）
     */
    fun getDeviceId(): String {
        return registrationCodeNative.getDeviceId()
    }

    /**
     * 清除VIP状态（用于测试）
     */
    fun clearVip() {
        registrationCodeNative.clearCode()
    }

    /**
     * 检查是否可以添加更多成员
     */
    fun canAddMember(currentCount: Int): Boolean {
        if (isPremiumActive()) return true
        return currentCount < FREE_MEMBER_LIMIT
    }

    /**
     * 检查是否可以使用某功能
     */
    fun canUseFeature(feature: PremiumFeature): Boolean {
        if (isPremiumActive()) return true
        return feature.isFree
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
    AI_USAGE(true, "AI 识别", "使用 AI 识别病历信息"),
    
    // 付费功能（需注册码激活）
    UNLIMITED_RECORDS(false, "无限记录", "无限制的就诊记录"),
    UNLIMITED_MEMBERS(false, "多成员", "管理多个家庭成员"),
    ADVANCED_REMINDER(false, "高级提醒", "复杂的重复提醒"),
    DATA_EXPORT(false, "数据导出", "导出数据为文件"),
    DATA_IMPORT(false, "数据导入", "从文件导入数据"),
    ENCRYPTED_BACKUP(false, "加密备份", "AES-256 加密备份"),
    GIST_SYNC(false, "云端同步", "同步到 GitHub Gist"),
    AI_CONFIGURATION(false, "AI 配置", "配置 AI 识别服务"),
    FULL_SECURITY(false, "完整安全", "全部安全功能")
}
