package com.noteapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.fragment.app.Fragment
import java.security.MessageDigest

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("note_security", Context.MODE_PRIVATE)

    // ─── PIN (Tạm thời không kích hoạt) ───────────────────────────────────────

    fun setPin(pin: String) {
        try {
            prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
        } catch (t: Throwable) {
            Log.e(TAG, "setPin error", t)
        }
    }

    fun verifyPin(pin: String): Boolean {
        return try {
            val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
            stored == hash(pin)
        } catch (t: Throwable) {
            Log.e(TAG, "verifyPin error", t)
            false
        }
    }

    fun hasPin(): Boolean {
        return try {
            prefs.contains(KEY_PIN_HASH)
        } catch (t: Throwable) {
            Log.e(TAG, "hasPin error", t)
            false
        }
    }

    fun clearPin() {
        try {
            prefs.edit().remove(KEY_PIN_HASH).apply()
        } catch (t: Throwable) {
            Log.e(TAG, "clearPin error", t)
        }
    }

    // ─── Biometric (Tạm thời tắt toàn bộ) ─────────────────────────────────────

    fun isBiometricAvailable(context: Context): Boolean {
        // Tắt toàn bộ kiểm tra vân tay để test khởi động trên Realme 2
        return false
    }

    fun showBiometricPrompt(
        fragment: Fragment,
        title: String = "Xác thực vân tay",
        subtitle: String = "Đặt ngón tay lên cảm biến",
        onSuccess: () -> Unit,
        onFailed: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Báo trực tiếp không khả dụng
        onError("Tính năng bảo mật vân tay tạm thời tắt")
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun hash(input: String): String = try {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        bytes.joinToString("") { "%02x".format(it) }
    } catch (t: Throwable) {
        Log.e(TAG, "hash error", t)
        input
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val TAG = "SecurityManager"
    }
}
