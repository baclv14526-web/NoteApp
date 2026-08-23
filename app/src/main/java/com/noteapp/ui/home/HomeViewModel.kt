package com.noteapp.ui.home

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.noteapp.data.db.entities.Category
import com.noteapp.data.db.entities.Note
import com.noteapp.data.db.entities.Tag
import com.noteapp.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: NoteRepository) : ViewModel() {

    data class FilterState(
        val categoryId: Long? = null,
        val tagId: Long? = null,
        val search: String = ""
    )

    private val _filter = MutableStateFlow(FilterState())
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: Flow<PagingData<Note>> = _filter
        .flatMapLatest { f ->
            repository.getPagedNotes(
                categoryId = f.categoryId,
                tagId = f.tagId,
                search = f.search
            )
        }
        .cachedIn(viewModelScope)

    val categories: LiveData<List<Category>> = repository.getAllCategories().asLiveData()
    val tags: LiveData<List<Tag>>            = repository.getAllTags().asLiveData()
    val noteCount: LiveData<Int>             = repository.getNoteCount().asLiveData()

    fun setSearch(query: String)    = _filter.update { it.copy(search = query) }
    fun setCategory(id: Long?)      = _filter.update { it.copy(categoryId = id) }
    fun setTag(id: Long?)           = _filter.update { it.copy(tagId = id) }
    fun clearFilters()              = _filter.value = FilterState()

    fun deleteNote(note: Note) = viewModelScope.launch { repository.deleteNote(note) }

    fun togglePin(note: Note) = viewModelScope.launch {
        repository.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }
}

class HomeViewModelFactory(private val repo: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
