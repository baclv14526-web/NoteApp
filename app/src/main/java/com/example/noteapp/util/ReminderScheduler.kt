package com.example.noteapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Lên lịch / huỷ nhắc nhở cho ghi chú bằng AlarmManager.
 *
 * Dùng AlarmManager (không phải WorkManager) vì đây là thông báo cần bắn
 * đúng thời điểm tuyệt đối do người dùng chọn (ví dụ "8:00 sáng mai") —
 * WorkManager tối ưu cho công việc nền định kỳ/linh hoạt, không đảm bảo
 * chạy đúng giờ chính xác như AlarmManager.setExactAndAllowWhileIdle.
 *
 * Mỗi ghi chú dùng requestCode = note.id.toInt() làm định danh alarm riêng,
 * nên đặt lại nhắc nhở cho note đã có alarm sẽ tự động thay thế alarm cũ.
 */
object ReminderScheduler {

    private fun pendingIntent(context: Context, noteId: Long, title: String, content: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
            putExtra(ReminderReceiver.EXTRA_NOTE_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_NOTE_CONTENT, content)
        }
        return PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Đặt lịch nhắc nhở tại [triggerAtMillis]. Trả về true nếu đặt thành công,
     * false nếu thiết bị không cho phép đặt exact alarm (Android 12+ yêu cầu
     * quyền SCHEDULE_EXACT_ALARM, người dùng có thể tắt trong Settings) —
     * trường hợp đó nên báo cho người dùng biết thay vì âm thầm không nhắc.
     */
    fun schedule(context: Context, noteId: Long, title: String, content: String, triggerAtMillis: Long): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return false
        }

        val pi = pendingIntent(context, noteId, title, content)
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } catch (e: SecurityException) {
            // Phòng trường hợp quyền bị thu hồi giữa lúc check và lúc đặt lịch.
            return false
        }
        return true
    }

    /** Huỷ nhắc nhở đã đặt cho ghi chú (nếu có). */
    fun cancel(context: Context, noteId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // title/content không quan trọng khi huỷ vì PendingIntent chỉ so khớp
        // theo requestCode + action + component, nhưng vẫn cần intent tương ứng.
        val pi = pendingIntent(context, noteId, "", "")
        alarmManager.cancel(pi)
        pi.cancel()
    }

    /** Thiết bị (Android 12+) có đang cho phép app đặt exact alarm hay không. */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }
}
