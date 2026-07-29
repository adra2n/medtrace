package com.yy.medtrace.data.backup

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val CURRENT_VERSION = 1
private const val ITERATIONS = 65536
private const val KEY_LENGTH = 256
private const val SALT_LENGTH = 16
private const val IV_LENGTH = 12
private const val GCM_TAG_LENGTH = 128

object CryptoUtil {
    private val random = SecureRandom()

    fun encrypt(plainText: String, password: String): String {
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val key = deriveKey(password, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        // Format: version(1) + iterations(4 big-endian) + salt + iv + encrypted
        val versionByte = byteArrayOf(CURRENT_VERSION.toByte())
        val iterBytes = byteArrayOf(
            ((ITERATIONS shr 24) and 0xFF).toByte(),
            ((ITERATIONS shr 16) and 0xFF).toByte(),
            ((ITERATIONS shr 8) and 0xFF).toByte(),
            (ITERATIONS and 0xFF).toByte()
        )
        val combined = versionByte + iterBytes + salt + iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String, password: String): String {
        val combined = Base64.decode(cipherText, Base64.NO_WRAP)
        // Detect format: version 1 starts with 0x01, old format starts with random salt
        val (iterations, offset) = if (combined.size > 5 && combined[0] == CURRENT_VERSION.toByte()) {
            val iter = ((combined[1].toInt() and 0xFF) shl 24) or
                    ((combined[2].toInt() and 0xFF) shl 16) or
                    ((combined[3].toInt() and 0xFF) shl 8) or
                    (combined[4].toInt() and 0xFF)
            iter to 5
        } else {
            ITERATIONS to 0  // Legacy format: no version prefix
        }
        val salt = combined.copyOfRange(offset, offset + SALT_LENGTH)
        val iv = combined.copyOfRange(offset + SALT_LENGTH, offset + SALT_LENGTH + IV_LENGTH)
        val encrypted = combined.copyOfRange(offset + SALT_LENGTH + IV_LENGTH, combined.size)
        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
