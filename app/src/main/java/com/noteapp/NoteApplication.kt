package com.noteapp

import android.app.Application
import com.noteapp.data.db.AppDatabase
import com.noteapp.data.repository.NoteRepository

class NoteApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { NoteRepository(database) }
}
