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
     */
    private fun setupCrashHandler() {
        try {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Log.e("NoteApp_CRASH", "=== UNCAUGHT EXCEPTION on thread: ${thread.name} ===", throwable)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        } catch (t: Throwable) {
            Log.e("NoteApp", "setupCrashHandler error", t)
        }
    }
}
