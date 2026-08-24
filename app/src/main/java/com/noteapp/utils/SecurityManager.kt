package com.noteapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.security.MessageDigest

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("note_security", Context.MODE_PRIVATE)

    // ─── PIN ─────────────────────────────────────────────────────────────────

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

    // ─── Biometric ───────────────────────────────────────────────────────────

    /**
     * Kiểm tra thiết bị có hỗ trợ vân tay không.
     * Dùng Throwable để bắt cả NoClassDefFoundError, NoSuchFieldError trên OEM Android 9.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        return try {
            val bm = BiometricManager.from(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+: BIOMETRIC_STRONG (0x000F) | BIOMETRIC_WEAK (0x00FF) = 255
                bm.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                ) == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                // API 28-29: canAuthenticate() không tham số
                @Suppress("DEPRECATION")
                bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
            }
        } catch (t: Throwable) {
            // Bắt Throwable (kể cả ClassNotFound / NoSuchMethod / VerifyError trên Oppo/Realme)
            Log.w(TAG, "isBiometricAvailable failed: ${t.message}")
            false
        }
    }

    /**
     * Hiển thị biometric prompt.
     * Bọc toàn bộ trong try-catch (t: Throwable) để không bao giờ gây crash app.
     */
    fun showBiometricPrompt(
        fragment: Fragment,
        title: String = "Xác thực vân tay",
        subtitle: String = "Đặt ngón tay lên cảm biến",
        onSuccess: () -> Unit,
        onFailed: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val context = fragment.context ?: run {
                onError("Không thể lấy ngữ cảnh ứng dụng")
                return
            }
            val executor = ContextCompat.getMainExecutor(context)

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    try { onSuccess() } catch (t: Throwable) {
                        Log.e(TAG, "onSuccess callback error", t)
                    }
                }

                override fun onAuthenticationFailed() {
                    try { onFailed() } catch (t: Throwable) {
                        Log.e(TAG, "onFailed callback error", t)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    try { onError(errString.toString()) } catch (t: Throwable) {
                        Log.e(TAG, "onError callback error", t)
                    }
                }
            }

            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Dùng mã PIN")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptInfoBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            }

            val promptInfo = promptInfoBuilder.build()
            BiometricPrompt(fragment, executor, callback).authenticate(promptInfo)

        } catch (t: Throwable) {
            Log.e(TAG, "showBiometricPrompt error caught", t)
            // Fallback an toàn: không crash, báo lỗi để fallback về mã PIN
            try {
                onError("Sinh trắc học không khả dụng, vui lòng dùng mã PIN")
            } catch (_: Throwable) {}
        }
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
