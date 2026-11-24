package com.example.noteapp.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Mở khoá bằng sinh trắc học (vân tay, và khuôn mặt nếu máy hỗ trợ ở mức "mạnh").
 *
 * Lưu ý bảo mật: đây là lớp tiện lợi đứng TRƯỚC mật khẩu 6 số, không thay thế nó.
 * Người dùng luôn có thể bấm "Dùng mã PIN" để nhập PIN như cũ, và PIN vẫn là
 * cách duy nhất để đổi mật khẩu hay đặt lại qua câu hỏi bảo mật.
 */
object BiometricHelper {

    enum class Availability { AVAILABLE, NO_HARDWARE, NOT_ENROLLED, UNAVAILABLE }

    fun availability(context: Context): Availability =
        when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Availability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NOT_ENROLLED
            else -> Availability.UNAVAILABLE
        }

    /** Máy có cảm biến và đã đăng ký ít nhất 1 vân tay/khuôn mặt hay chưa. */
    fun isAvailable(context: Context): Boolean = availability(context) == Availability.AVAILABLE

    /** Tìm FragmentActivity từ Context của Compose (thường bị bọc trong ContextWrapper). */
    fun findActivity(context: Context): FragmentActivity? {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is FragmentActivity) return c
            c = c.baseContext
        }
        return null
    }

    /**
     * Hiện hộp thoại xác thực sinh trắc học của hệ thống.
     *
     * [onError] nhận (thông báo, người dùng tự huỷ?). Khi người dùng bấm nút phụ
     * ("Dùng mã PIN") hoặc đóng hộp thoại thì canceledByUser = true — màn gọi nên
     * im lặng quay về ô nhập PIN, không báo lỗi.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (message: String, canceledByUser: Boolean) -> Unit = { _, _ -> }
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val canceled = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                onError(errString.toString(), canceled)
            }
            // onAuthenticationFailed (vân tay không khớp): hộp thoại hệ thống tự báo
            // "Không nhận dạng được" và cho thử lại, không cần xử lý thêm.
        }

        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { if (subtitle != null) setSubtitle(subtitle) }
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(info)
    }
}
