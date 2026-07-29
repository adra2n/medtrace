package com.yy.medtrace.data

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
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
        private const val TAG = "RegistrationCodeNative"
        private const val PREFS_NAME = "registration_prefs"
        private const val KEY_REGISTRATION_CODE = "registration_code"
        
        init {
            try {
                System.loadLibrary("license_verify")
                Log.d(TAG, "✅ NDK 库加载成功")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ NDK 库加载失败", e)
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
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        Log.d(TAG, "设备ID: $deviceId")
        return deviceId
    }

    /**
     * 验证注册码
     */
    fun verifyCode(code: String): Boolean {
        return try {
            val deviceId = getDeviceId()
            Log.d(TAG, "输入的注册码: $code")
            Log.d(TAG, "当前设备ID: $deviceId")
            
            val result = verifyLicense(code, deviceId)
            val isValid = result == 1
            
            if (isValid) {
                Log.d(TAG, "✅ 注册码验证成功")
            } else {
                Log.w(TAG, "❌ 注册码验证失败")
            }
            
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "注册码验证异常", e)
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
