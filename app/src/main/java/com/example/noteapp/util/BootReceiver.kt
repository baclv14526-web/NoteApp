package com.example.noteapp.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.noteapp.NoteApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AlarmManager bị hệ điều hành huỷ sạch mỗi khi thiết bị khởi động lại, nên
 * cần lắng nghe BOOT_COMPLETED để tự đặt lại các nhắc nhở còn hiệu lực
 * (reminderAt trong tương lai) đọc từ database.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContext = context.applicationContext
        val repository = (appContext as NoteApplication).repository

        // BroadcastReceiver.onReceive() chạy trên main thread và bị hệ thống
        // giới hạn thời gian, nên chỉ dùng một scope ngắn hạn ở đây để đọc DB
        // rồi đặt lại alarm — không launch tác vụ dài hơi.
        CoroutineScope(Dispatchers.IO).launch {
            val activeReminders = repository.getActiveReminders()
            activeReminders.forEach { note ->
                ReminderScheduler.schedule(
                    context = appContext,
                    noteId = note.id,
                    title = note.title,
                    content = note.content,
                    triggerAtMillis = note.reminderAt!!
                )
            }
        }
    }
}
