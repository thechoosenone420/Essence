package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("text", text)
            put("isChecked", isChecked)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): ChecklistItem {
            return ChecklistItem(
                id = obj.optString("id", UUID.randomUUID().toString()),
                text = obj.optString("text", ""),
                isChecked = obj.optBoolean("isChecked", false)
            )
        }

        fun parseList(jsonStr: String): List<ChecklistItem> {
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

        fun encodeList(list: List<ChecklistItem>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            return array.toString()
        }
    }
}

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String = "",
    val checklistJson: String = "",
    val tagsJson: String = "",
    val folder: String = "General",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isVoiceNote: Boolean = false,
    val audioPath: String? = null,
    val audioDurationMs: Long = 0L,
    val audioAmplitudesJson: String = "",
    val transcript: String = "",
    val transcriptionStatus: String = "NONE", // NONE, PENDING, COMPLETED, FAILED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getChecklist(): List<ChecklistItem> = ChecklistItem.parseList(checklistJson)

    fun getTags(): List<String> {
        if (tagsJson.isBlank()) return emptyList()
        return try {
            val array = JSONArray(tagsJson)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAmplitudes(): List<Int> {
        if (audioAmplitudesJson.isBlank()) return emptyList()
        return try {
            val array = JSONArray(audioAmplitudesJson)
            (0 until array.length()).map { array.getInt(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
