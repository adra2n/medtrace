package com.yy.chiyaole.data.backup

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val ITERATIONS = 65536
private const val KEY_LENGTH = 256
private const val SALT_LENGTH = 16
private const val IV_LENGTH = 12
private const val GCM_TAG_LENGTH = 128

object CryptoUtil {
    private val random = SecureRandom()

    fun encrypt(plainText: String, password: String): String {
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = salt + iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String, password: String): String {
        val combined = Base64.decode(cipherText, Base64.NO_WRAP)
        val salt = combined.copyOfRange(0, SALT_LENGTH)
        val iv = combined.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
        val encrypted = combined.copyOfRange(SALT_LENGTH + IV_LENGTH, combined.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
