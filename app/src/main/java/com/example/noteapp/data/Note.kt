package com.example.noteapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Một ghi chú.
 * - bgColorHex / textColorHex: màu nền và màu chữ dạng "#RRGGBB"
 * - bgImageUri: nếu khác null, ảnh (png/jpg/jpeg) sẽ được dùng làm nền
 *   thay cho bgColorHex.
 * - category: 1 category cho mỗi ghi chú (ví dụ "Công việc", "Cá nhân"...)
 * - tags: nhiều tag, lưu dạng chuỗi phân tách bởi dấu phẩy, ví dụ "urgent,idea"
 * - isDeleted / deletedAt: soft-delete cho tính năng Thùng rác. Ghi chú xoá
 *   không bị xoá khỏi DB ngay mà chỉ ẩn khỏi danh sách chính, có thể khôi
 *   phục trong vòng 30 ngày trước khi bị dọn vĩnh viễn.
 * - reminderAt: thời điểm (epoch millis) sẽ bắn thông báo nhắc nhở, null nếu
 *   ghi chú không đặt nhắc nhở. Xem ReminderScheduler để biết cách lên lịch.
 * - isLocked: ghi chú "bí mật" — ẩn title/content ở danh sách chính và trên
 *   Widget, yêu cầu nhập đúng PIN 6 số (chung cho cả app, xem PinManager)
 *   trước khi mở xem hoặc sửa.
 * - editCount: số lần ghi chú đã được lưu/sửa kể từ lúc tạo (không tính lần
 *   tạo đầu tiên). Tăng tự động trong NoteRepository.save() mỗi khi note đã
 *   tồn tại (có id > 0) được lưu lại.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val bgColorHex: String = "#FFF9C4",
    val textColorHex: String = "#000000",
    val bgImageUri: String? = null,
    val category: String = "Chung",
    val tags: String = "",
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val reminderAt: Long? = null,
    val isLocked: Boolean = false,
    val editCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val tagList: List<String>
        get() = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
