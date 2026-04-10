package com.hativ2.util

/**
 * Centralized input validation for user-facing forms.
 *
 * Why a separate object instead of inline validation in each screen:
 *   - Single source of truth for validation rules.
 *   - Consistent error messages across AddExpenseScreen, DashboardDialog, etc.
 *   - Easier to update limits without hunting through UI code.
 *
 * Why these specific limits:
 *   - 100 chars for names/descriptions: long enough for real use, short enough to
 *     prevent abuse and UI overflow.
 *   - 1,000,000 max amount: covers any realistic expense while preventing overflow
 *     in Double arithmetic (Double can represent integers exactly up to 2^53).
 *   - 0.01 min amount: matches the smallest currency unit (centavo/cent).
 */
object InputValidator {

    const val MAX_NAME_LENGTH = 100
    const val MAX_DESCRIPTION_LENGTH = 100
    const val MAX_CATEGORY_LENGTH = 50
    const val MAX_AMOUNT = 1_000_000.0
    const val MIN_AMOUNT = 0.01

    /**
     * Sanitizes a user-entered string by trimming whitespace and collapsing
     * consecutive whitespace characters to a single space.
     *
     * Why collapse whitespace instead of just trim:
     *   - Users sometimes accidentally add multiple spaces or paste text with
     *     newlines embedded. This prevents visual inconsistencies in the UI
     *     and prevents "   " (all-whitespace) from passing isBlank checks.
     */
    fun sanitize(input: String): String {
        return input.trim().replace(Regex("\\s+"), " ")
    }

    fun validateDashboardTitle(title: String): ValidationResult {
        val sanitized = sanitize(title)
        return when {
            sanitized.isBlank() -> ValidationResult.Invalid("Title cannot be empty")
            sanitized.length > MAX_NAME_LENGTH -> ValidationResult.Invalid("Title must be $MAX_NAME_LENGTH characters or fewer")
            else -> ValidationResult.Valid(sanitized)
        }
    }

    fun validatePersonName(name: String): ValidationResult {
        val sanitized = sanitize(name)
        return when {
            sanitized.isBlank() -> ValidationResult.Invalid("Name cannot be empty")
            sanitized.length > MAX_NAME_LENGTH -> ValidationResult.Invalid("Name must be $MAX_NAME_LENGTH characters or fewer")
            else -> ValidationResult.Valid(sanitized)
        }
    }

    fun validateExpenseDescription(description: String): ValidationResult {
        val sanitized = sanitize(description)
        return when {
            sanitized.isBlank() -> ValidationResult.Invalid("Description cannot be empty")
            sanitized.length > MAX_DESCRIPTION_LENGTH -> ValidationResult.Invalid("Description must be $MAX_DESCRIPTION_LENGTH characters or fewer")
            else -> ValidationResult.Valid(sanitized)
        }
    }

    fun validateAmount(amountString: String): AmountValidationResult {
        val trimmed = amountString.trim()
        if (trimmed.isBlank()) {
            return AmountValidationResult.Invalid("Amount cannot be empty")
        }
        val amount = trimmed.toDoubleOrNull()
            ?: return AmountValidationResult.Invalid("Invalid number")
        return when {
            amount < MIN_AMOUNT -> AmountValidationResult.Invalid("Amount must be at least $MIN_AMOUNT")
            amount > MAX_AMOUNT -> AmountValidationResult.Invalid("Amount cannot exceed ${MAX_AMOUNT.toLong()}")
            amount.isNaN() || amount.isInfinite() -> AmountValidationResult.Invalid("Invalid amount")
            else -> AmountValidationResult.Valid(amount)
        }
    }

    sealed interface ValidationResult {
        data class Valid(val sanitized: String) : ValidationResult
        data class Invalid(val error: String) : ValidationResult
    }

    sealed interface AmountValidationResult {
        data class Valid(val amount: Double) : AmountValidationResult
        data class Invalid(val error: String) : AmountValidationResult
    }
}
