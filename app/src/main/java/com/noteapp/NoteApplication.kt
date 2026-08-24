package com.noteapp

import android.app.Application
import android.util.Log
import com.noteapp.data.db.AppDatabase
import com.noteapp.data.repository.NoteRepository

class NoteApplication : Application() {
    companion object {
        private const val TAG = "NoteApplication"
    }

    val database by lazy {
        try {
            Log.d(TAG, "Initializing database...")
            AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize database", e)
            throw e
        }
    }

    val repository by lazy {
        try {
            Log.d(TAG, "Initializing repository...")
            NoteRepository(database)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize repository", e)
            throw e
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate started")
        try {
            // Pre-initialize database and repository
            database
            repository
            Log.d(TAG, "Application initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Application initialization failed", e)
            throw e
        }
    }
}
