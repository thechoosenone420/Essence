package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getTrashNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteByIdFlow(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isVoiceNote = 1 ORDER BY updatedAt DESC")
    fun getVoiceNotes(): Flow<List<Note>>

    @Query("SELECT DISTINCT folder FROM notes WHERE isDeleted = 0 AND folder != ''")
    fun getFolders(): Flow<List<String>>

    @Query("""
        SELECT * FROM notes 
        WHERE isDeleted = 0 
          AND isArchived = 0 
          AND (title LIKE '%' || :query || '%' 
               OR body LIKE '%' || :query || '%' 
               OR transcript LIKE '%' || :query || '%' 
               OR tagsJson LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesDirect(): List<Note>
}
