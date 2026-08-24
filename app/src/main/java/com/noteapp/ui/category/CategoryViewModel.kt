package com.noteapp.ui.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.noteapp.data.db.entities.Category
import com.noteapp.data.db.entities.Tag
import com.noteapp.data.repository.NoteRepository
import kotlinx.coroutines.launch

class CategoryViewModel(private val repository: NoteRepository) : ViewModel() {

    val categories: LiveData<List<Category>> = repository.getAllCategories().asLiveData()
    val tags: LiveData<List<Tag>>            = repository.getAllTags().asLiveData()

    fun insertCategory(name: String, color: Int) {
        viewModelScope.launch { repository.insertCategory(Category(name = name, color = color)) }
    }

    fun updateCategory(cat: Category) {
        viewModelScope.launch { repository.updateCategory(cat) }
    }

    fun deleteCategory(cat: Category) {
        viewModelScope.launch { repository.deleteCategory(cat) }
    }

    fun insertTag(name: String, color: Int) {
        viewModelScope.launch { repository.insertTag(Tag(name = name, color = color)) }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch { repository.updateTag(tag) }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch { repository.deleteTag(tag) }
    }
}

class CategoryViewModelFactory(private val repo: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoryViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
