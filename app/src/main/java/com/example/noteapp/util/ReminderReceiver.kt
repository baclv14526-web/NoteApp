package com.example.noteapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.noteapp.MainActivity
import com.example.noteapp.R

/**
 * Nhận tín hiệu từ AlarmManager khi đến giờ nhắc nhở và hiển thị notification.
 * Tap vào notification sẽ mở thẳng MainActivity tới đúng ghi chú đó.
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_NOTE_TITLE = "extra_note_title"
        const val EXTRA_NOTE_CONTENT = "extra_note_content"
        const val CHANNEL_ID = "note_reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId == -1L) return

        val title = intent.getStringExtra(EXTRA_NOTE_TITLE)?.ifBlank { "Nhắc nhở ghi chú" } ?: "Nhắc nhở ghi chú"
        val content = intent.getStringExtra(EXTRA_NOTE_CONTENT).orEmpty()

        ensureChannel(context)

        // Deep-link: mở thẳng vào màn hình sửa ghi chú tương ứng khi tap notification.
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_NOTE_ID, noteId)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content.ifBlank { "Chạm để xem ghi chú" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        // Kiểm tra quyền POST_NOTIFICATIONS (bắt buộc từ Android 13+) trước khi
        // gọi notify — nếu người dùng đã từ chối quyền, notify() sẽ ném
        // SecurityException nên cần bọc an toàn thay vì crash app.
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(context).notify(noteId.toInt(), notification)
            } catch (e: SecurityException) {
                // Quyền bị thu hồi ngay trước khi gọi notify — bỏ qua, không crash.
            }
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc nhở ghi chú",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Thông báo nhắc nhở cho các ghi chú đã đặt giờ"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
