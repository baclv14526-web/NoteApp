package com.example.noteapp.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * PagingSource được Room tự sinh dựa trên câu query bên dưới.
 * Paging3 chỉ load từng trang (xem cấu hình PagingConfig trong Repository)
 * thay vì load hết một lúc, nên vẫn mượt khi số lượng ghi chú > 100.
 *
 * :query      -> chuỗi tìm kiếm ("" nghĩa là không lọc theo từ khoá)
 * :category   -> "Tất cả" nghĩa là không lọc theo category
 * :sortOption -> tên của SortOption enum (UPDATED_DESC, TITLE_ASC, ...)
 *
 * Ghi chú đã bị xoá (isDeleted = 1) luôn bị loại khỏi danh sách chính.
 * Ghi chú ghim (isPinned) luôn ưu tiên lên đầu bất kể sort theo kiểu gì —
 * dùng CASE WHEN vì Room không cho bind tên cột động vào ORDER BY.
 *
 * Ghi chú bí mật (isLocked = 1) vẫn hiện trong danh sách duyệt bình thường
 * (query rỗng) để người dùng biết nó tồn tại, nhưng KHÔNG bao giờ match khi
 * tìm kiếm theo từ khoá — nếu cho phép, kẻ tấn công có thể dò nội dung bí
 * mật bằng cách thử nhiều từ khoá và quan sát note có xuất hiện hay không
 * (side-channel qua kết quả tìm kiếm), dù không đọc được nội dung thật.
 */
@Dao
interface NoteDao {

    @Query(
        """
        SELECT * FROM notes
        WHERE isDeleted = 0
        AND (
            :query = ''
            OR (
                isLocked = 0
                AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
            )
        )
        AND (:category = 'Tất cả' OR category = :category)
        ORDER BY
            isPinned DESC,
            CASE WHEN :sortOption = 'UPDATED_DESC' THEN updatedAt END DESC,
            CASE WHEN :sortOption = 'UPDATED_ASC' THEN updatedAt END ASC,
            CASE WHEN :sortOption = 'CREATED_DESC' THEN createdAt END DESC,
            CASE WHEN :sortOption = 'CREATED_ASC' THEN createdAt END ASC,
            CASE WHEN :sortOption = 'TITLE_ASC' THEN title END ASC,
            CASE WHEN :sortOption = 'TITLE_DESC' THEN title END DESC,
            updatedAt DESC
        """
    )
    fun pagingSource(query: String, category: String, sortOption: String): PagingSource<Int, Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    // Lấy toàn bộ ghi chú chưa xoá (không phân trang) — dùng cho tính năng Export.
    // Không dùng cho danh sách chính vì sẽ load hết vào RAM một lúc.
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllNotesForExport(): List<Note>

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 0")
    fun countAll(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note): Long

    // Import hàng loạt ghi chú từ file JSON đã export trước đó.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(notes: List<Note>): List<Long>

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ── Thùng rác (soft-delete) ────────────────────────────────────────────

    /** Chuyển ghi chú vào thùng rác thay vì xoá thẳng. */
    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long)

    /** Khôi phục ghi chú từ thùng rác về danh sách chính. */
    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashedNotes(): Flow<List<Note>>

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 1")
    fun countTrashed(): Flow<Int>

    /** Xoá vĩnh viễn các ghi chú đã ở trong thùng rác quá lâu (dọn dẹp tự động). */
    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :beforeTimestamp")
    suspend fun purgeOldTrash(beforeTimestamp: Long)

    /** Xoá vĩnh viễn toàn bộ thùng rác ngay lập tức. */
    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    // ── Categories ──────────────────────────────────────────────────────────
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)

    // ── Tags (để build danh sách gợi ý) ────────────────────────────────────
    // Loại trừ ghi chú bí mật để tránh lộ tag của note khoá qua danh sách gợi ý.
    @Query("SELECT tags FROM notes WHERE tags != '' AND isDeleted = 0 AND isLocked = 0")
    suspend fun getAllTagStrings(): List<String>

    // ── Nhắc nhở (Reminder) ─────────────────────────────────────────────────

    /** Cập nhật/xoá giờ nhắc nhở của một ghi chú. Truyền null để huỷ nhắc nhở. */
    @Query("UPDATE notes SET reminderAt = :reminderAt WHERE id = :id")
    suspend fun setReminder(id: Long, reminderAt: Long?)

    /**
     * Toàn bộ ghi chú đang có nhắc nhở còn hiệu lực (chưa xoá, chưa qua giờ hẹn).
     * Dùng để lên lịch lại alarm sau khi máy khởi động lại (BOOT_COMPLETED),
     * vì AlarmManager bị Android tự huỷ hết mỗi lần reboot.
     */
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND reminderAt IS NOT NULL AND reminderAt > :now")
    suspend fun getActiveReminders(now: Long): List<Note>

    // ── Widget (App Widget hiển thị ghi chú ghim) ──────────────────────────
    /**
     * Danh sách ghi chú đã ghim, mới sửa nhất lên đầu — dùng cho App Widget.
     * Ghi chú đã khoá (isLocked = 1) LUÔN bị loại khỏi widget dù có ghim,
     * vì tap vào 1 dòng trong widget mở thẳng vào note mà không qua bước
     * nhập PIN — hiển thị note bí mật ở đây sẽ vô hiệu hoá tính năng khoá.
     */
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isPinned = 1 AND isLocked = 0 ORDER BY updatedAt DESC")
    fun getPinnedNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isPinned = 1 AND isLocked = 0 ORDER BY updatedAt DESC")
    suspend fun getPinnedNotesOnce(): List<Note>

    // ── Ghi chú bí mật (khoá PIN) ───────────────────────────────────────────
    @Query("UPDATE notes SET isLocked = :isLocked WHERE id = :id")
    suspend fun setLocked(id: Long, isLocked: Boolean)
}
