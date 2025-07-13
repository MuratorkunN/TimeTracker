package com.example.roboticsgenius

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val noteDao = db.noteDao()

    val allNotes: StateFlow<List<Note>> = noteDao.getAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createNote(title: String, color: String, onNoteCreated: (Int) -> Unit) {
        viewModelScope.launch {
            val newNote = Note(
                title = title,
                content = "",
                color = color,
                lastModified = System.currentTimeMillis()
            )
            val newId = noteDao.insert(newNote)
            onNoteCreated(newId.toInt())
        }
    }

    suspend fun getNoteById(noteId: Int): Note? {
        return noteDao.getNoteById(noteId)
    }

    fun updateNote(noteId: Int, newTitle: String, newContent: String) {
        viewModelScope.launch {
            val note = noteDao.getNoteById(noteId)
            note?.let {
                it.title = newTitle
                it.content = newContent
                it.lastModified = System.currentTimeMillis()
                noteDao.update(it)
            }
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            noteDao.deleteNoteById(noteId)
        }
    }
}