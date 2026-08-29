package com.example.noteapp

import android.app.Application
import com.example.noteapp.data.AppDatabase
import com.example.noteapp.data.NoteRepository

class NoteApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { NoteRepository(database.noteDao(), applicationContext) }
}
