package com.example.noteapp.data

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.noteapp.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [applicationContext] chỉ dùng để gọi WidgetUpdater.refresh() sau các thao
 * tác ghi có thể ảnh hưởng tới ghi chú ghim hiển thị trên Home Screen widget.
 * Đây là Application Context nên không có rủi ro leak (sống suốt vòng đời app).
 */
class NoteRepository(private val dao: NoteDao, private val applicationContext: Context) {

    /**
     * Trả về dòng dữ liệu phân trang. Paging3 tự load thêm khi người dùng
     * cuộn gần hết trang hiện tại (prefetchDistance), giữ bộ nhớ ổn định
     * dù cơ sở dữ liệu có hàng nghìn ghi chú.
     */
    fun getNotesPaged(query: String, category: String, sortOption: SortOption): Flow<PagingData<Note>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 6,
                enablePlaceholders = false,
                initialLoadSize = 40
            ),
            pagingSourceFactory = { dao.pagingSource(query.trim(), category, sortOption.name) }
        ).flow
    }

    val noteCount: Flow<Int> = dao.countAll()
    val categories: Flow<List<Category>> = dao.getCategories()

    /**
     * Map "tên category" -> "số ghi chú", kèm sẵn key ALL_CATEGORIES ("Tất cả")
     * là tổng toàn bộ ghi chú chưa xoá — dùng để hiện "Công việc (5)" trên chip.
     * Category chưa có ghi chú nào sẽ không xuất hiện trong map (UI tự hiểu
     * là 0 khi không tìm thấy key, xem NoteListScreen/NoteEditScreen).
     */
    val categoryCounts: Flow<Map<String, Int>> = dao.getCategoryCounts().map { list ->
        val byCategory = list.associate { it.name to it.count }
        val total = byCategory.values.sum()
        byCategory + (ALL_CATEGORIES to total)
    }

    suspend fun getById(id: Long) = dao.getById(id)

    /**
     * Lưu ghi chú rồi làm mới widget — vì thao tác này có thể vừa mới ghim/bỏ
     * ghim note, hoặc sửa nội dung của một note đang được ghim và hiển thị
     * trên Home Screen.
     *
     * editCount được tự tính lại ở đây (không tin giá trị UI truyền vào):
     * nếu note.id == 0 (tạo mới lần đầu) thì editCount giữ 0; nếu note đã tồn
     * tại trong DB thì lấy editCount hiện có + 1. Cách này tránh bug nếu màn
     * hình sửa quên đồng bộ editCount khi copy dữ liệu từ note gốc.
     */
    suspend fun save(note: Note): Long {
        val effectiveEditCount = if (note.id == 0L) {
            0
        } else {
            val existing = dao.getById(note.id)
            (existing?.editCount ?: 0) + 1
        }
        val id = dao.upsert(note.copy(editCount = effectiveEditCount, updatedAt = System.currentTimeMillis()))
        WidgetUpdater.refresh(applicationContext)
        return id
    }

    suspend fun addCategory(name: String, colorHex: String) =
        dao.insertCategory(Category(name = name, colorHex = colorHex))
    suspend fun deleteCategory(category: Category) = dao.deleteCategory(category)

    suspend fun getAllTags(): List<String> =
        dao.getAllTagStrings()
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

    // ── Thùng rác ───────────────────────────────────────────────────────────

    /** Xoá "mềm": chuyển ghi chú vào thùng rác, có thể khôi phục sau đó. */
    suspend fun moveToTrash(note: Note) {
        dao.moveToTrash(note.id, System.currentTimeMillis())
        WidgetUpdater.refresh(applicationContext)
    }

    suspend fun restoreFromTrash(note: Note) {
        dao.restoreFromTrash(note.id)
        WidgetUpdater.refresh(applicationContext)
    }

    val trashedNotes: Flow<List<Note>> = dao.getTrashedNotes()
    val trashCount: Flow<Int> = dao.countTrashed()

    /** Xoá vĩnh viễn một ghi chú (dùng trong màn Thùng rác). */
    suspend fun deletePermanently(note: Note) {
        dao.delete(note)
        WidgetUpdater.refresh(applicationContext)
    }

    suspend fun emptyTrash() {
        dao.emptyTrash()
        WidgetUpdater.refresh(applicationContext)
    }

    /** Tự động dọn các ghi chú đã nằm trong thùng rác quá [retentionDays] ngày. */
    suspend fun purgeOldTrash(retentionDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - retentionDays * 24L * 60 * 60 * 1000
        dao.purgeOldTrash(cutoff)
    }

    // ── Export / Import ────────────────────────────────────────────────────
    /**
     * [excludeLocked] mặc định true: ghi chú bí mật (isLocked) sẽ KHÔNG được
     * đưa vào file export .txt/.pdf/.json — vì các file này không mã hoá,
     * xuất note bí mật ra sẽ vô hiệu hoá hoàn toàn mục đích của tính năng khoá.
     * Người dùng có thể chủ động chọn bao gồm nếu thực sự muốn (UI sẽ cảnh báo).
     */
    suspend fun getAllNotesForExport(excludeLocked: Boolean = true): List<Note> {
        val all = dao.getAllNotesForExport()
        return if (excludeLocked) all.filterNot { it.isLocked } else all
    }

    /** Import danh sách ghi chú, trả về số lượng ghi chú mới được thêm thành công. */
    suspend fun importNotes(notes: List<Note>): Int {
        // id = 0 để Room tự sinh id mới, tránh đè lên ghi chú hiện có khi import.
        val toInsert = notes.map { it.copy(id = 0) }
        val result = dao.insertAll(toInsert).count { it != -1L }
        if (result > 0) WidgetUpdater.refresh(applicationContext)
        return result
    }

    // ── Nhắc nhở (Reminder) ─────────────────────────────────────────────────
    suspend fun setReminder(noteId: Long, reminderAt: Long?) = dao.setReminder(noteId, reminderAt)
    suspend fun getActiveReminders(): List<Note> = dao.getActiveReminders(System.currentTimeMillis())

    // ── Widget ──────────────────────────────────────────────────────────────
    val pinnedNotesFlow: Flow<List<Note>> = dao.getPinnedNotesFlow()
    suspend fun getPinnedNotesOnce(): List<Note> = dao.getPinnedNotesOnce()

    // ── Ghi chú bí mật (khoá PIN) ────────────────────────────────────────────
    /** Khoá/mở khoá một ghi chú. Làm mới widget vì note bị khoá sẽ tự động
     *  biến mất khỏi widget dù đang ghim (xem NoteDao.getPinnedNotesFlow). */
    suspend fun setLocked(noteId: Long, isLocked: Boolean) {
        dao.setLocked(noteId, isLocked)
        WidgetUpdater.refresh(applicationContext)
    }
}
