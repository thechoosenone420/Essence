package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val activeNotes: Flow<List<Note>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()
    val trashNotes: Flow<List<Note>> = noteDao.getTrashNotes()
    val voiceNotes: Flow<List<Note>> = noteDao.getVoiceNotes()
    val folders: Flow<List<String>> = noteDao.getFolders()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    fun getNoteByIdFlow(id: Long): Flow<Note?> = noteDao.getNoteByIdFlow(id)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))

    suspend fun togglePin(note: Note) {
        noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleArchive(note: Note) {
        noteDao.updateNote(note.copy(isArchived = !note.isArchived, updatedAt = System.currentTimeMillis()))
    }

    suspend fun moveToTrash(note: Note) {
        noteDao.updateNote(note.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
    }

    suspend fun restoreFromTrash(note: Note) {
        noteDao.updateNote(note.copy(isDeleted = false, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deletePermanently(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    suspend fun getAllNotesDirect(): List<Note> = noteDao.getAllNotesDirect()
}
