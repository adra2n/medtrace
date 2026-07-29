package com.yy.medtrace.data

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NDK 原生注册码验证
 * 
 * 安全性:
 * - 密钥编译到 .so 文件中
 * - 反编译 .so 比反编译 Java/Kotlin 难度大得多
 * - 需要逆向 ARM 汇编才能提取密钥
 */
@Singleton
class RegistrationCodeNative @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "registration_prefs"
        private const val KEY_REGISTRATION_CODE = "registration_code"
        
        init {
            try {
                System.loadLibrary("license_verify")
            } catch (e: UnsatisfiedLinkError) {
                // Native library load failure handled in verifyCode via catch(Throwable)
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    /**
     * JNI 原生方法：验证注册码
     */
    private external fun verifyLicense(inputCode: String, androidId: String): Int

    /**
     * JNI 原生方法：获取设备哈希（调试用）
     */
    private external fun getDeviceHash(androidId: String): String

    /**
     * 获取当前设备ID
     */
    fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    /**
     * 验证注册码
     */
    fun verifyCode(code: String): Boolean {
        return try {
            val deviceId = getDeviceId()
            val result = verifyLicense(code, deviceId)
            result == 1
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * 保存注册码
     */
    fun saveCode(code: String) {
        prefs.edit()
            .putString(KEY_REGISTRATION_CODE, code.trim().uppercase())
            .apply()
    }

    /**
     * 获取已保存的注册码
     */
    fun getSavedCode(): String {
        return prefs.getString(KEY_REGISTRATION_CODE, "") ?: ""
    }

    /**
     * 清除注册码
     */
    fun clearCode() {
        prefs.edit()
            .remove(KEY_REGISTRATION_CODE)
            .apply()
    }

    /**
     * 检查是否已激活VIP
     */
    fun isVipActive(): Boolean {
        val savedCode = getSavedCode()
        if (savedCode.isBlank()) return false
        return verifyCode(savedCode)
    }

    /**
     * 获取设备哈希（调试用）
     */
    fun getDeviceHashDebug(): String {
        return getDeviceHash(getDeviceId())
    }
}
