package com.example.noteapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * Yêu cầu Glance vẽ lại toàn bộ instance của PinnedNotesWidget đang có trên
 * Home Screen. Gọi hàm này mỗi khi danh sách ghi chú ghim có thể đã thay đổi
 * (ghim/bỏ ghim, sửa nội dung note đang ghim, xoá note đang ghim...).
 *
 * updateAll() tự kiểm tra nếu người dùng chưa thêm widget nào thì không làm
 * gì cả — an toàn để gọi vô điều kiện mà không cần kiểm tra trước.
 */
object WidgetUpdater {
    suspend fun refresh(context: Context) {
        PinnedNotesWidget().updateAll(context)
    }
}
