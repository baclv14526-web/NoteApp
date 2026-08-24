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
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return stored == hash(pin)
    }

    fun hasPin(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun clearPin() = prefs.edit().remove(KEY_PIN_HASH).apply()

    // ─── Biometric ───────────────────────────────────────────────────────────

    /**
     * Kiểm tra thiết bị có hỗ trợ vân tay không.
     * - API 30+ : dùng Authenticators constants (BIOMETRIC_STRONG | BIOMETRIC_WEAK)
     * - API 28-29: dùng canAuthenticate() không tham số (deprecated nhưng an toàn)
     * - Bọc try-catch để tránh crash trên OEM lạ (Realme, Oppo ColorOS...)
     */
    fun isBiometricAvailable(context: Context): Boolean {
        return try {
            val bm = BiometricManager.from(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+
                bm.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                ) == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                // API 28-29: canAuthenticate() không tham số
                @Suppress("DEPRECATION")
                bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
            }
        } catch (e: Exception) {
            Log.w(TAG, "isBiometricAvailable: ${e.message}")
            false
        }
    }

    /**
     * Hiển thị biometric prompt.
     * - API 30+ : dùng setAllowedAuthenticators()
     * - API 28-29: KHÔNG gọi setAllowedAuthenticators() (API chưa tồn tại)
     * - Toàn bộ bọc try-catch để không crash khi OEM có bug
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
            val executor = ContextCompat.getMainExecutor(fragment.requireContext())

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    try { onSuccess() } catch (e: Exception) {
                        Log.e(TAG, "onSuccess callback error", e)
                    }
                }

                override fun onAuthenticationFailed() {
                    try { onFailed() } catch (e: Exception) {
                        Log.e(TAG, "onFailed callback error", e)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    try { onError(errString.toString()) } catch (e: Exception) {
                        Log.e(TAG, "onError callback error", e)
                    }
                }
            }

            val promptInfo: BiometricPrompt.PromptInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // API 30+: dùng setAllowedAuthenticators
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setNegativeButtonText("Dùng mã PIN")
                        .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                        )
                        .build()
                } else {
                    // API 28-29: KHÔNG dùng setAllowedAuthenticators
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setNegativeButtonText("Dùng mã PIN")
                        .build()
                }

            BiometricPrompt(fragment, executor, callback).authenticate(promptInfo)

        } catch (e: Exception) {
            Log.e(TAG, "showBiometricPrompt failed", e)
            // Fallback: báo lỗi để UI chuyển về nhập PIN
            onError("Vân tay không khả dụng trên thiết bị này")
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun hash(input: String): String = try {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        bytes.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        Log.e(TAG, "hash error", e)
        input // fallback (không nên xảy ra)
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val TAG = "SecurityManager"
    }
}
