package com.hativ2.util

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.PersonEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportManagerTest {

    private val defaultPeople = listOf(
        PersonEntity("user-current", "You", "default", 1000L),
        PersonEntity("u1", "Alice", "blue", 1000L),
        PersonEntity("u2", "Bob", "red", 1000L)
    )

    @Test
    fun `generateCsv produces correct header`() {
        val csv = CsvExportManager.generateCsv(emptyList(), emptyList())
        val lines = csv.lines()
        assertEquals("Date,Description,Category,Amount,Paid By,Split Details", lines[0])
    }

    @Test
    fun `generateCsv returns only header for empty expense list`() {
        val csv = CsvExportManager.generateCsv(emptyList(), emptyList())
        val lines = csv.trimEnd('\n').lines()
        assertEquals(1, lines.size)
    }

    @Test
    fun `generateCsv includes expense data`() {
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "Lunch", 100.0, "u1", "food", 1609459200000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, defaultPeople)
        val lines = csv.trimEnd('\n').lines()
        assertEquals(2, lines.size) // header + 1 expense

        val dataLine = lines[1]
        assertTrue(dataLine.contains("Lunch"))
        assertTrue(dataLine.contains("100.00"))
        assertTrue(dataLine.contains("Alice"))
        assertTrue(dataLine.contains("Food")) // category is title-cased
    }

    @Test
    fun `generateCsv resolves current user to You`() {
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "Coffee", 5.0, "user-current", "drinks", 1000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, defaultPeople)
        assertTrue(csv.contains("You"))
    }

    @Test
    fun `generateCsv shows Unknown for missing person`() {
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "Mystery", 50.0, "unknown-id", "misc", 1000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, defaultPeople)
        assertTrue(csv.contains("Unknown"))
    }

    @Test
    fun `generateCsv escapes description containing comma`() {
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "Food, drinks", 30.0, "u1", "food", 1000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, defaultPeople)
        // Description should be wrapped in quotes
        assertTrue(csv.contains("\"Food, drinks\""))
    }

    @Test
    fun `generateCsv escapes description containing double quotes`() {
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "The \"best\" meal", 30.0, "u1", "food", 1000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, defaultPeople)
        // Quotes should be doubled and wrapped
        assertTrue(csv.contains("\"The \"\"best\"\" meal\""))
    }

    @Test
    fun `generateCsv escapes description containing newline`() {
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "Line1\nLine2", 30.0, "u1", "food", 1000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, defaultPeople)
        // Should be wrapped in quotes
        assertTrue(csv.contains("\"Line1\nLine2\""))
    }

    @Test
    fun `generateCsv formats amount with two decimal places`() {
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "Snack", 9.5, "u1", "food", 1000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, defaultPeople)
        assertTrue(csv.contains("9.50"))
    }

    @Test
    fun `generateCsv sorts expenses by descending date`() {
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "First", 10.0, "u1", "food", 1000L),
            ExpenseEntity("e2", "d1", "Second", 20.0, "u1", "food", 3000L),
            ExpenseEntity("e3", "d1", "Third", 30.0, "u1", "food", 2000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, defaultPeople)
        val lines = csv.trimEnd('\n').lines()
        // Header + 3 data rows
        assertEquals(4, lines.size)
        // Second (3000L) should come first, Third (2000L) second, First (1000L) third
        assertTrue(lines[1].contains("Second"))
        assertTrue(lines[2].contains("Third"))
        assertTrue(lines[3].contains("First"))
    }

    @Test
    fun `generateCsv title-cases category`() {
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "Test", 10.0, "u1", "transport", 1000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, defaultPeople)
        assertTrue(csv.contains("Transport"))
    }

    @Test
    fun `generateCsv includes Shared as split details`() {
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "Test", 10.0, "u1", "food", 1000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, defaultPeople)
        assertTrue(csv.contains("Shared"))
    }

    @Test
    fun `generateCsv handles multiple expenses`() {
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "Lunch", 100.0, "u1", "food", 1000L),
            ExpenseEntity("e2", "d1", "Dinner", 200.0, "u2", "food", 2000L),
            ExpenseEntity("e3", "d1", "Taxi", 50.0, "user-current", "transport", 3000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, defaultPeople)
        val lines = csv.trimEnd('\n').lines()
        assertEquals(4, lines.size)
    }

    @Test
    fun `generateCsv escapes payer name containing comma`() {
        val people = listOf(
            PersonEntity("u1", "Smith, John", "blue", 1000L)
        )
        val expenses = listOf(
            ExpenseEntity("e1", "d1", "Test", 10.0, "u1", "food", 1000L)
        )
        val csv = CsvExportManager.generateCsv(expenses, people)
        assertTrue(csv.contains("\"Smith, John\""))
    }
}
