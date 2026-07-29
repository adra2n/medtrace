package com.yy.medtrace.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 注册码管理器
 * 使用 RSA 非对称加密验证注册码
 */
@Singleton
class RegistrationCodeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "RegistrationCodeManager"
        private const val PREFS_NAME = "registration_prefs"
        private const val KEY_REGISTRATION_CODE = "registration_code"
        
        // RSA 公钥 (嵌入App)
        // 由 keygen.py 生成，对应的私钥用于生成注册码
        private const val PUBLIC_KEY = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsjoEL8YOrJQsL52TrtUG
gI/gmanc06HaoXZRdH/uGKzioUpSmaY+m+O16Ro+kFparHLqixvzloAAydVYolNy
mO9GsUBhfThc4Y+86Mo5i1mpZgmBCTCjEdOZx9O1RAqXx+iA6PnxTaX/D/SaSPWA
WG5z+bJnaDxwG5bsEt2jEyJ4tHG/N8+q/3TYRxN6YKrMp+enky8f/zL53NrRwdya
ehWyJjcoE3w4lt9Szsd5+INTRhBHkYrsINvQH1iN7L1tKDiUGAdjO4KnqXFz7PmJ
TsxDyulg6IZsOk1Kfa75BqaPq62HNMYeaR87XP0m7cJ6eaEMjS+kkc1i4e+zHL26
jwIDAQAB
-----END PUBLIC KEY-----"""
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

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
            // 清理输入
            val cleanCode = code.trim().replace("-", "")
            Log.d(TAG, "输入的注册码: $code")
            Log.d(TAG, "清理后: $cleanCode")
            
            // 解析公钥
            val publicKey = parsePublicKey(PUBLIC_KEY)
            
            // Base64 解码
            val decoded = Base64.decode(cleanCode, Base64.DEFAULT)
            Log.d(TAG, "Base64解码后字节数: ${decoded.size}")
            
            // RSA 解密
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            cipher.init(Cipher.DECRYPT_MODE, publicKey)
            val decryptedBytes = cipher.doFinal(decoded)
            val decrypted = String(decryptedBytes, Charsets.UTF_8)
            Log.d(TAG, "解密结果: $decrypted")
            
            // 验证设备ID
            val deviceId = getDeviceId()
            Log.d(TAG, "当前设备ID: $deviceId")
            val isValid = decrypted == deviceId
            
            if (isValid) {
                Log.d(TAG, "✅ 注册码验证成功")
            } else {
                Log.w(TAG, "❌ 注册码验证失败: 设备ID不匹配")
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
     * 每次启动都会重新验证
     */
    fun isVipActive(): Boolean {
        val savedCode = getSavedCode()
        if (savedCode.isBlank()) return false
        return verifyCode(savedCode)
    }

    /**
     * 解析公钥字符串
     */
    private fun parsePublicKey(keyStr: String): RSAPublicKey {
        val cleanKey = keyStr
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        
        val keyBytes = Base64.decode(cleanKey, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec) as RSAPublicKey
    }
}
