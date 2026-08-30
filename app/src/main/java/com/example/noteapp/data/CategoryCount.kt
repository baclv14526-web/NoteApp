package com.example.noteapp.data

/**
 * Kết quả projection cho query đếm số ghi chú theo category
 * (NoteDao.getCategoryCounts). Tên field phải khớp chính xác với alias
 * trong câu SQL ("name", "count") để Room tự map đúng.
 */
data class CategoryCount(
    val name: String,
    val count: Int
)
