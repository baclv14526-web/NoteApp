package com.noteapp

import android.app.Application
import android.util.Log
import com.noteapp.data.db.AppDatabase
import com.noteapp.data.repository.NoteRepository

class NoteApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { NoteRepository(database) }

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    /**
     * Bắt uncaught exception toàn cục, log ra Logcat trước khi crash.
     * Giúp đọc được lỗi qua adb logcat ngay cả khi không có crash reporter.
     */
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("NoteApp_CRASH", "=== UNCAUGHT EXCEPTION on thread: ${thread.name} ===", throwable)
            // Chuyển về handler mặc định để hệ thống xử lý bình thường
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
