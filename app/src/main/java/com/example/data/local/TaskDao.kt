package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY isDone ASC, priority ASC, dueDate ASC, updatedAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isDone = 0 ORDER BY priority ASC, dueDate ASC, updatedAt DESC")
    fun getActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isDone = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskByIdFlow(id: Long): Flow<Task?>

    @Query("SELECT DISTINCT project FROM tasks WHERE isDeleted = 0 AND project != ''")
    fun getProjects(): Flow<List<String>>

    @Query("""
        SELECT * FROM tasks 
        WHERE isDeleted = 0 
          AND (title LIKE '%' || :query || '%' 
               OR notes LIKE '%' || :query || '%' 
               OR tagsJson LIKE '%' || :query || '%')
        ORDER BY isDone ASC, priority ASC, dueDate ASC
    """)
    fun searchTasks(query: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isDone = 0 AND reminderTimesJson != '' AND reminderTimesJson != '[]'")
    suspend fun getTasksWithReminders(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksDirect(): List<Task>
}
