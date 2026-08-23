package com.noteapp.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.noteapp.data.db.AppDatabase
import com.noteapp.data.db.entities.*
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val db: AppDatabase) {

    private val noteDao     = db.noteDao()
    private val categoryDao = db.categoryDao()
    private val tagDao      = db.tagDao()

    // ─── Notes ───────────────────────────────────────────────────────────────

    fun getPagedNotes(
        categoryId: Long? = null,
        tagId: Long? = null,
        search: String = ""
    ): Flow<PagingData<Note>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            prefetchDistance = 5,
            enablePlaceholders = false,
            initialLoadSize = 40
        ),
        pagingSourceFactory = { noteDao.getAllNotesPaged(categoryId, tagId, search) }
    ).flow

    suspend fun getNoteWithTags(id: Long): NoteWithTags? = noteDao.getNoteWithTags(id)
    suspend fun getNoteById(id: Long): Note?             = noteDao.getNoteById(id)
    suspend fun insertNote(note: Note): Long             = noteDao.insertNote(note)
    suspend fun updateNote(note: Note)                   = noteDao.updateNote(note)
    suspend fun deleteNote(note: Note)                   = noteDao.deleteNote(note)
    suspend fun deleteNoteById(id: Long)                 = noteDao.deleteNoteById(id)
    fun getNoteCount(): Flow<Int>                        = noteDao.getNoteCount()
    suspend fun getAllNotesForExport(): List<NoteWithTags> = noteDao.getAllNotesForExport()

    // ─── Tags per note ───────────────────────────────────────────────────────

    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef) =
        tagDao.insertNoteTagCrossRef(crossRef)

    suspend fun deleteTagsForNote(noteId: Long) = tagDao.deleteTagsForNote(noteId)

    // ─── Categories ──────────────────────────────────────────────────────────

    fun getAllCategories(): Flow<List<Category>>       = categoryDao.getAllCategories()
    suspend fun getAllCategoriesSync(): List<Category> = categoryDao.getAllCategoriesSync()
    suspend fun insertCategory(c: Category): Long     = categoryDao.insertCategory(c)
    suspend fun updateCategory(c: Category)           = categoryDao.updateCategory(c)
    suspend fun deleteCategory(c: Category)           = categoryDao.deleteCategory(c)

    // ─── Tags ─────────────────────────────────────────────────────────────────

    fun getAllTags(): Flow<List<Tag>>             = tagDao.getAllTags()
    suspend fun getAllTagsSync(): List<Tag>       = tagDao.getAllTagsSync()
    suspend fun getTagsForNote(id: Long): List<Tag> = tagDao.getTagsForNote(id)
    suspend fun insertTag(t: Tag): Long          = tagDao.insertTag(t)
    suspend fun updateTag(t: Tag)                = tagDao.updateTag(t)
    suspend fun deleteTag(t: Tag)                = tagDao.deleteTag(t)
}
