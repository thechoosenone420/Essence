package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SubtaskItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("text", text)
            put("isDone", isDone)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): SubtaskItem {
            return SubtaskItem(
                id = obj.optString("id", UUID.randomUUID().toString()),
                text = obj.optString("text", ""),
                isDone = obj.optBoolean("isDone", false)
            )
        }

        fun parseList(jsonStr: String): List<SubtaskItem> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                (0 until array.length()).map { i ->
                    fromJson(array.getJSONObject(i))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun encodeList(list: List<SubtaskItem>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            return array.toString()
        }
    }
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val subtasksJson: String = "",
    val priority: Int = 3, // 1: Urgent (Red), 2: High, 3: Medium, 4: Low
    val tagsJson: String = "",
    val project: String = "Inbox",
    val dueDate: Long? = null, // epoch millis for date
    val dueTimeMinutes: Int? = null, // minutes from midnight (0..1439)
    val isDone: Boolean = false,
    val completedAt: Long? = null,
    val reminderTimesJson: String = "", // JSON array of timestamps for alarms
    val repeatRule: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY, YEARLY, WEEKDAYS
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getSubtasks(): List<SubtaskItem> = SubtaskItem.parseList(subtasksJson)

    fun getTags(): List<String> {
        if (tagsJson.isBlank()) return emptyList()
        return try {
            val array = JSONArray(tagsJson)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getReminderTimes(): List<Long> {
        if (reminderTimesJson.isBlank()) return emptyList()
        return try {
            val array = JSONArray(reminderTimesJson)
            (0 until array.length()).map { array.getLong(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
