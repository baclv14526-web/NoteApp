package com.noteapp.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.noteapp.data.db.entities.Note
import com.noteapp.data.db.entities.NoteWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // BUG FIX: Thay Long? bằng Long + sentinel -1L để tránh lỗi Room KSP
    // nullable primitive trong @Query gây lỗi code generation với một số version KSP
    @Query("""
        SELECT DISTINCT n.* FROM notes n
        LEFT JOIN note_tag_cross_ref ntcr ON n.id = ntcr.noteId
        WHERE (:categoryId = -1 OR n.categoryId = :categoryId)
          AND (:tagId = -1 OR ntcr.tagId = :tagId)
          AND (:search = '' OR n.title LIKE '%' || :search || '%'
               OR n.content LIKE '%' || :search || '%')
        ORDER BY n.isPinned DESC, n.updatedAt DESC
    """)
    fun getAllNotesPaged(
        categoryId: Long,
        tagId: Long,
        search: String
    ): PagingSource<Int, Note>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteWithTags(id: Long): NoteWithTags?

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("SELECT COUNT(*) FROM notes")
    fun getNoteCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllNotesForExport(): List<NoteWithTags>
}
