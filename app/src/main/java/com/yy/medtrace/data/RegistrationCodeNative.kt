package com.yy.medtrace.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.yy.medtrace.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
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
        private const val DEBUG_SIGNATURE_HASH = "846d78dc4d4da3c00715050fac83f87154c898e38640a97cd6839a71b3260870"
        private const val RELEASE_SIGNATURE_HASH = "d82600c18237ac321e22a8e5afb2c187bd9b7a2f96c15f64f997df54e78ed728"
        
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
     * 获取当前设备ID
     */
    fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    /**
     * 校验 APK 签名完整性（防篡改）
     */
    private fun checkIntegrity(): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            if (signatures.isNullOrEmpty()) return false
            
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signatures[0].toByteArray())
            val signatureHash = digest.joinToString("") { "%02x".format(it) }
            
            val expectedHash = if (BuildConfig.DEBUG) {
                DEBUG_SIGNATURE_HASH
            } else {
                RELEASE_SIGNATURE_HASH
            }
            
            signatureHash == expectedHash
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 验证注册码
     */
    fun verifyCode(code: String): Boolean {
        return try {
            if (!checkIntegrity()) return false
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
        if (!checkIntegrity()) return false
        val savedCode = getSavedCode()
        if (savedCode.isBlank()) return false
        return verifyCode(savedCode)
    }
}
