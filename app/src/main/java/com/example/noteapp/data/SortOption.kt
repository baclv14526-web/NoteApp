package com.example.noteapp.data

/**
 * Các kiểu sắp xếp danh sách ghi chú. Ghi chú ghim (isPinned) luôn được ưu
 * tiên hiển thị lên đầu bất kể chọn kiểu sắp xếp nào — xem NoteDao.pagingSource.
 */
enum class SortOption(val label: String) {
    UPDATED_DESC("Mới sửa nhất"),
    UPDATED_ASC("Cũ sửa nhất"),
    CREATED_DESC("Mới tạo nhất"),
    CREATED_ASC("Cũ tạo nhất"),
    TITLE_ASC("Tên A → Z"),
    TITLE_DESC("Tên Z → A")
}
