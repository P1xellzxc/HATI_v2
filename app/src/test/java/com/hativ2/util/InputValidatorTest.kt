package com.hativ2.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    // --- sanitize ---

    @Test
    fun `sanitize trims leading and trailing whitespace`() {
        assertEquals("hello", InputValidator.sanitize("  hello  "))
    }

    @Test
    fun `sanitize collapses consecutive internal whitespace`() {
        assertEquals("hello world", InputValidator.sanitize("hello   world"))
    }

    @Test
    fun `sanitize handles mixed whitespace characters`() {
        assertEquals("a b c", InputValidator.sanitize("  a  b  c  "))
    }

    @Test
    fun `sanitize returns empty for whitespace-only input`() {
        assertEquals("", InputValidator.sanitize("   "))
    }

    // --- validateDashboardTitle ---

    @Test
    fun `validateDashboardTitle rejects empty input`() {
        val result = InputValidator.validateDashboardTitle("")
        assertTrue(result is InputValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateDashboardTitle rejects whitespace-only input`() {
        val result = InputValidator.validateDashboardTitle("   ")
        assertTrue(result is InputValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateDashboardTitle accepts valid title`() {
        val result = InputValidator.validateDashboardTitle("Trip to Japan")
        assertTrue(result is InputValidator.ValidationResult.Valid)
        assertEquals("Trip to Japan", (result as InputValidator.ValidationResult.Valid).sanitized)
    }

    @Test
    fun `validateDashboardTitle rejects title exceeding max length`() {
        val longTitle = "a".repeat(InputValidator.MAX_NAME_LENGTH + 1)
        val result = InputValidator.validateDashboardTitle(longTitle)
        assertTrue(result is InputValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateDashboardTitle accepts title at exact max length`() {
        val maxTitle = "a".repeat(InputValidator.MAX_NAME_LENGTH)
        val result = InputValidator.validateDashboardTitle(maxTitle)
        assertTrue(result is InputValidator.ValidationResult.Valid)
    }

    // --- validatePersonName ---

    @Test
    fun `validatePersonName rejects empty name`() {
        val result = InputValidator.validatePersonName("")
        assertTrue(result is InputValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validatePersonName accepts valid name and sanitizes`() {
        val result = InputValidator.validatePersonName("  John  Doe  ")
        assertTrue(result is InputValidator.ValidationResult.Valid)
        assertEquals("John Doe", (result as InputValidator.ValidationResult.Valid).sanitized)
    }

    // --- validateExpenseDescription ---

    @Test
    fun `validateExpenseDescription rejects blank description`() {
        val result = InputValidator.validateExpenseDescription("  ")
        assertTrue(result is InputValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateExpenseDescription accepts normal description`() {
        val result = InputValidator.validateExpenseDescription("Dinner at restaurant")
        assertTrue(result is InputValidator.ValidationResult.Valid)
    }

    // --- validateAmount ---

    @Test
    fun `validateAmount rejects empty string`() {
        val result = InputValidator.validateAmount("")
        assertTrue(result is InputValidator.AmountValidationResult.Invalid)
    }

    @Test
    fun `validateAmount rejects non-numeric input`() {
        val result = InputValidator.validateAmount("abc")
        assertTrue(result is InputValidator.AmountValidationResult.Invalid)
    }

    @Test
    fun `validateAmount rejects zero`() {
        val result = InputValidator.validateAmount("0")
        assertTrue(result is InputValidator.AmountValidationResult.Invalid)
    }

    @Test
    fun `validateAmount rejects negative amount`() {
        val result = InputValidator.validateAmount("-10")
        assertTrue(result is InputValidator.AmountValidationResult.Invalid)
    }

    @Test
    fun `validateAmount rejects amount exceeding max`() {
        val result = InputValidator.validateAmount("1000001")
        assertTrue(result is InputValidator.AmountValidationResult.Invalid)
    }

    @Test
    fun `validateAmount accepts valid amount`() {
        val result = InputValidator.validateAmount("150.50")
        assertTrue(result is InputValidator.AmountValidationResult.Valid)
        assertEquals(150.50, (result as InputValidator.AmountValidationResult.Valid).amount, 0.001)
    }

    @Test
    fun `validateAmount accepts minimum valid amount`() {
        val result = InputValidator.validateAmount("0.01")
        assertTrue(result is InputValidator.AmountValidationResult.Valid)
    }

    @Test
    fun `validateAmount accepts max amount exactly`() {
        val result = InputValidator.validateAmount("1000000")
        assertTrue(result is InputValidator.AmountValidationResult.Valid)
    }

    @Test
    fun `validateAmount rejects amount just below minimum`() {
        val result = InputValidator.validateAmount("0.001")
        assertTrue(result is InputValidator.AmountValidationResult.Invalid)
    }
}
