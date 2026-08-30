package com.example.noteapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Giá trị đặc biệt cho category = "không lọc theo category nào cả". */
const val ALL_CATEGORIES = "Tất cả"

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#90CAF9"
)
