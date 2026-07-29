package com.yy.medtrace.data.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinManager {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val PIN_LENGTH = 6
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L

    @Volatile
    private var failedAttempts = 0
    @Volatile
    private var lockoutUntil = 0L

    fun isLocked(): Boolean = System.currentTimeMillis() < lockoutUntil

    fun getLockoutRemainingSeconds(): Int {
        val remaining = (lockoutUntil - System.currentTimeMillis()) / 1000
        return remaining.coerceAtLeast(0).toInt()
    }

    fun resetAttempts() {
        failedAttempts = 0
        lockoutUntil = 0L
    }

    fun isPinSet(context: Context): Boolean =
        !SecurePrefs.get(context).getString(SecurePrefs.PIN_HASH, null).isNullOrBlank()

    fun setPin(context: Context, pin: String) {
        require(pin.length == PIN_LENGTH && pin.all { it.isDigit() }) {
            "PIN 需为 $PIN_LENGTH 位数字"
        }
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        val value = Base64.encodeToString(salt, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(hash, Base64.NO_WRAP)
        SecurePrefs.get(context).edit().putString(SecurePrefs.PIN_HASH, value).apply()
        resetAttempts()
    }

    fun verify(context: Context, pin: String): Boolean {
        if (isLocked()) return false
        val stored = SecurePrefs.get(context).getString(SecurePrefs.PIN_HASH, null) ?: return false
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expected = Base64.decode(parts[1], Base64.NO_WRAP)
        val actual = derive(pin, salt)
        val match = constantTimeEquals(expected, actual)
        if (match) {
            resetAttempts()
        } else {
            failedAttempts++
            if (failedAttempts >= MAX_ATTEMPTS) {
                lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                failedAttempts = 0
            }
        }
        return match
    }

    fun clearPin(context: Context) {
        SecurePrefs.get(context).edit().remove(SecurePrefs.PIN_HASH).apply()
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
