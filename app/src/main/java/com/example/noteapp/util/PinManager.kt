package com.example.noteapp.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Quản lý PIN 6 số dùng chung cho toàn app để mở các ghi chú "bí mật"
 * (Note.isLocked = true), cùng với câu hỏi bảo mật để reset PIN khi quên.
 *
 * Không bao giờ lưu PIN hoặc câu trả lời dạng plaintext — chỉ lưu SHA-256
 * hash kèm salt ngẫu nhiên. Toàn bộ SharedPreferences được mã hoá bằng
 * Android Keystore thông qua EncryptedSharedPreferences (Jetpack Security),
 * nên kể cả khi thiết bị bị root/trích xuất file, dữ liệu vẫn không đọc được
 * nếu không có key phần cứng của chính thiết bị đó.
 */
object PinManager {

    private const val PREFS_NAME = "secure_notes_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SALT = "pin_salt"
    private const val KEY_QUESTION = "security_question"
    private const val KEY_ANSWER_HASH = "answer_hash"
    private const val KEY_ANSWER_SALT = "answer_salt"

    /** Danh sách câu hỏi bảo mật cho người dùng chọn khi đặt PIN lần đầu. */
    val securityQuestions = listOf(
        "Tên thú cưng đầu tiên của bạn là gì?",
        "Tên trường tiểu học của bạn là gì?",
        "Món ăn yêu thích của bạn là gì?",
        "Tên thành phố bạn sinh ra là gì?",
        "Biệt danh thời nhỏ của bạn là gì?"
    )

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hash(value: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + value).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** App đã từng đặt PIN hay chưa. */
    fun isPinSet(context: Context): Boolean =
        prefs(context).contains(KEY_PIN_HASH)

    /**
     * Đặt PIN mới (6 số) kèm câu hỏi + câu trả lời bảo mật để reset sau này.
     * Ghi đè PIN cũ nếu đã có (dùng khi đổi PIN hoặc reset PIN quên).
     */
    fun setPin(context: Context, pin: String, question: String, answer: String) {
        val pinSalt = randomSalt()
        val answerSalt = randomSalt()
        // Câu trả lời bảo mật không phân biệt hoa/thường và bỏ khoảng trắng
        // thừa để người dùng dễ nhớ lại chính xác khi cần reset.
        val normalizedAnswer = answer.trim().lowercase()

        prefs(context).edit()
            .putString(KEY_PIN_HASH, hash(pin, pinSalt))
            .putString(KEY_PIN_SALT, pinSalt)
            .putString(KEY_QUESTION, question)
            .putString(KEY_ANSWER_HASH, hash(normalizedAnswer, answerSalt))
            .putString(KEY_ANSWER_SALT, answerSalt)
            .apply()
    }

    /** Kiểm tra PIN người dùng nhập có khớp với PIN đã lưu không. */
    fun verifyPin(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val storedHash = p.getString(KEY_PIN_HASH, null) ?: return false
        val salt = p.getString(KEY_PIN_SALT, null) ?: return false
        return hash(pin, salt) == storedHash
    }

    /** Câu hỏi bảo mật đã lưu, null nếu chưa từng đặt PIN. */
    fun getSecurityQuestion(context: Context): String? =
        prefs(context).getString(KEY_QUESTION, null)

    /** Kiểm tra câu trả lời bảo mật người dùng nhập để cho phép reset PIN. */
    fun verifyAnswer(context: Context, answer: String): Boolean {
        val p = prefs(context)
        val storedHash = p.getString(KEY_ANSWER_HASH, null) ?: return false
        val salt = p.getString(KEY_ANSWER_SALT, null) ?: return false
        val normalizedAnswer = answer.trim().lowercase()
        return hash(normalizedAnswer, salt) == storedHash
    }

    /** Xoá PIN đã đặt — dùng khi người dùng muốn tắt hẳn tính năng khoá ghi chú. */
    fun clearPin(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
