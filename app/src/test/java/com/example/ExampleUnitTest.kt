package com.example

import com.example.data.model.ChecklistItem
import com.example.data.model.SubtaskItem
import com.example.util.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun checklistItem_serialization_isCorrect() {
        val list = listOf(
            ChecklistItem(id = "1", text = "Buy Milk", isChecked = false),
            ChecklistItem(id = "2", text = "Call Alex", isChecked = true)
        )
        val json = ChecklistItem.encodeList(list)
        val parsed = ChecklistItem.parseList(json)

        assertEquals(2, parsed.size)
        assertEquals("Buy Milk", parsed[0].text)
        assertEquals(false, parsed[0].isChecked)
        assertEquals("Call Alex", parsed[1].text)
        assertEquals(true, parsed[1].isChecked)
    }

    @Test
    fun subtaskItem_serialization_isCorrect() {
        val list = listOf(
            SubtaskItem(id = "sub_1", text = "Draft outline", isDone = true),
            SubtaskItem(id = "sub_2", text = "Review notes", isDone = false)
        )
        val json = SubtaskItem.encodeList(list)
        val parsed = SubtaskItem.parseList(json)

        assertEquals(2, parsed.size)
        assertEquals(true, parsed[0].isDone)
        assertEquals(false, parsed[1].isDone)
    }

    @Test
    fun computeNextDueDate_daily_isCorrect() {
        val now = System.currentTimeMillis()
        val next = AlarmScheduler.computeNextDueDate(now, "DAILY")
        assertNotNull(next)
        assertTrue(next!! > now)
    }
}
