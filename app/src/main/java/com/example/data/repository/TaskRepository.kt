package com.example.data.repository

import com.example.data.local.TaskDao
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val activeTasks: Flow<List<Task>> = taskDao.getActiveTasks()
    val completedTasks: Flow<List<Task>> = taskDao.getCompletedTasks()
    val projects: Flow<List<String>> = taskDao.getProjects()

    fun searchTasks(query: String): Flow<List<Task>> = taskDao.searchTasks(query)

    fun getTaskByIdFlow(id: Long): Flow<Task?> = taskDao.getTaskByIdFlow(id)

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))

    suspend fun toggleDone(task: Task) {
        val newDone = !task.isDone
        taskDao.updateTask(
            task.copy(
                isDone = newDone,
                completedAt = if (newDone) System.currentTimeMillis() else null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun getTasksWithReminders(): List<Task> = taskDao.getTasksWithReminders()

    suspend fun getAllTasksDirect(): List<Task> = taskDao.getAllTasksDirect()
}
