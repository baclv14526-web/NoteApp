package com.noteapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.noteapp.data.db.dao.CategoryDao
import com.noteapp.data.db.dao.NoteDao
import com.noteapp.data.db.dao.TagDao
import com.noteapp.data.db.entities.*

@Database(
    entities = [Note::class, Category::class, Tag::class, NoteTagCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "note_database"
                ).fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
    }
}
