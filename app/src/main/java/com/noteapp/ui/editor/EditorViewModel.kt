package com.noteapp.ui.editor

import androidx.lifecycle.*
import com.noteapp.data.db.entities.*
import com.noteapp.data.repository.NoteRepository
import kotlinx.coroutines.launch

class EditorViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _note       = MutableLiveData<Note>()
    val note: LiveData<Note> = _note

    private val _tags       = MutableLiveData<List<Tag>>(emptyList())
    val tags: LiveData<List<Tag>> = _tags

    val allCategories: LiveData<List<Category>> = repository.getAllCategories().asLiveData()
    val allTags: LiveData<List<Tag>>            = repository.getAllTags().asLiveData()

    var currentNoteId: Long = -1L
        private set

    fun loadNote(id: Long) {
        currentNoteId = id
        if (id <= 0L) {
            _note.value = Note()
            _tags.value = emptyList()
            return
        }
        viewModelScope.launch {
            val nwt = repository.getNoteWithTags(id)
            if (nwt != null) {
                _note.value  = nwt.note
                _tags.value  = nwt.tags
            } else {
                _note.value = Note()
            }
        }
    }

    fun updateBackground(color: Int) { _note.value = _note.value?.copy(backgroundColor = color) }
    fun updateTextColor(color: Int)  { _note.value = _note.value?.copy(textColor = color) }
    fun updateBgImage(uri: String?)  { _note.value = _note.value?.copy(backgroundImageUri = uri) }
    fun updateCategory(id: Long?)    { _note.value = _note.value?.copy(categoryId = id) }
    fun toggleSecure()               { _note.value = _note.value?.copy(isSecure = !(_note.value?.isSecure ?: false)) }
    fun togglePin()                  { _note.value = _note.value?.copy(isPinned = !(_note.value?.isPinned ?: false)) }

    fun setSelectedTags(tags: List<Tag>) { _tags.value = tags }

    fun saveNote(title: String, content: String, onDone: (Long) -> Unit) {
        val base = _note.value ?: Note()
        val now  = System.currentTimeMillis()
        val toSave = base.copy(
            title     = title,
            content   = content,
            updatedAt = now,
            createdAt = if (base.id == 0L) now else base.createdAt
        )
        viewModelScope.launch {
            val id = if (toSave.id == 0L) {
                repository.insertNote(toSave)
            } else {
                repository.updateNote(toSave); toSave.id
            }
            // Save tags
            repository.deleteTagsForNote(id)
            _tags.value?.forEach { tag ->
                repository.insertNoteTagCrossRef(NoteTagCrossRef(id, tag.id))
            }
            currentNoteId = id
            onDone(id)
        }
    }

    fun deleteNote(onDone: () -> Unit) {
        val n = _note.value ?: return
        viewModelScope.launch {
            if (n.id > 0) repository.deleteNote(n)
            onDone()
        }
    }
}

class EditorViewModelFactory(private val repo: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditorViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
