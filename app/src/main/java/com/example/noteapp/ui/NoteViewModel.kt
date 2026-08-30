package com.example.noteapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.noteapp.data.ALL_CATEGORIES
import com.example.noteapp.data.Category
import com.example.noteapp.data.Note
import com.example.noteapp.data.NoteRepository
import com.example.noteapp.data.SortOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    init {
        // Dọn tự động các ghi chú đã nằm trong thùng rác quá 30 ngày, chạy
        // một lần mỗi khi app khởi động ViewModel (không cần WorkManager
        // cho một tác vụ nhẹ và không bắt buộc chạy nền khi app đóng).
        viewModelScope.launch {
            repository.purgeOldTrash(retentionDays = 30)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow(ALL_CATEGORIES)
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _sortOption = MutableStateFlow(SortOption.UPDATED_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption

    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** "Tên category" -> "số ghi chú", đã kèm sẵn tổng cho ALL_CATEGORIES. */
    val categoryCounts: StateFlow<Map<String, Int>> = repository.categoryCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val noteCount: StateFlow<Int> = repository.noteCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trashCount: StateFlow<Int> = repository.trashCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val notesPaged: Flow<PagingData<Note>> =
        combine(_searchQuery, _selectedCategory, _sortOption) { q, c, s -> Triple(q, c, s) }
            .flatMapLatest { (q, c, s) -> repository.getNotesPaged(q, c, s) }
            .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val trashedNotesPaged: Flow<List<Note>> = repository.trashedNotes

    fun onSearchQueryChange(value: String) {
        _searchQuery.value = value
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onSortOptionSelected(option: SortOption) {
        _sortOption.value = option
    }

    fun saveNote(note: Note, onDone: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repository.save(note)
        onDone(id)
    }

    /** Xoá "mềm": chuyển ghi chú vào thùng rác, có thể khôi phục sau. */
    fun moveToTrash(note: Note) = viewModelScope.launch {
        repository.moveToTrash(note)
    }

    fun restoreFromTrash(note: Note) = viewModelScope.launch {
        repository.restoreFromTrash(note)
    }

    /** Xoá vĩnh viễn — chỉ dùng trong màn hình Thùng rác. */
    fun deletePermanently(note: Note) = viewModelScope.launch {
        repository.deletePermanently(note)
    }

    fun emptyTrash() = viewModelScope.launch {
        repository.emptyTrash()
    }

    fun addCategory(name: String, colorHex: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.addCategory(name.trim(), colorHex)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        repository.deleteCategory(category)
    }

    suspend fun getNoteById(id: Long): Note? = repository.getById(id)
    suspend fun getAllTags(): List<String> = repository.getAllTags()

    // ── Nhắc nhở (Reminder) ─────────────────────────────────────────────────
    // Lưu ý: chỉ lưu reminderAt vào DB ở đây. Việc gọi AlarmManager (schedule/
    // cancel) cần Context nên được thực hiện trực tiếp ở NoteEditScreen —
    // ViewModel không nên giữ Context để tránh leak vòng đời.
    fun setReminder(noteId: Long, reminderAt: Long?) = viewModelScope.launch {
        repository.setReminder(noteId, reminderAt)
    }

    // ── Export / Import ────────────────────────────────────────────────────

    /** Lấy toàn bộ ghi chú để export. Mặc định loại trừ ghi chú bí mật. */
    suspend fun getAllNotesForExport(excludeLocked: Boolean = true): List<Note> =
        repository.getAllNotesForExport(excludeLocked)

    /** Import danh sách note đã parse từ file JSON, trả kết quả qua callback. */
    fun importNotes(notes: List<Note>, onResult: (Int) -> Unit) = viewModelScope.launch {
        val count = repository.importNotes(notes)
        onResult(count)
    }

    // ── Ghi chú bí mật (khoá PIN) ────────────────────────────────────────────
    fun setLocked(noteId: Long, isLocked: Boolean) = viewModelScope.launch {
        repository.setLocked(noteId, isLocked)
    }
}

class NoteViewModelFactory(private val repository: NoteRepository) :
    androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NoteViewModel(repository) as T
    }
}
