package com.hativ2.domain.usecase

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.data.entity.SplitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateDebtsUseCaseTest {

    private val useCase = CalculateDebtsUseCase()
    private val currentUserId = "user-current"

    @Test
    fun `calculate simple 2-person equal split`() {
        // Scenario: You pay 100, split equally with Alice
        // Result: Alice owes You 50
        val expenses = listOf(
            createExpense(id = "e1", amount = 100.0, paidBy = currentUserId)
        )
        val splits = listOf(
            createSplit(expenseId = "e1", personId = currentUserId, amount = 50.0),
            createSplit(expenseId = "e1", personId = "Alice", amount = 50.0)
        )
        val userIds = listOf(currentUserId, "Alice")

        val result = useCase.execute(expenses, splits, emptyList(), userIds, currentUserId)

        assertEquals(50.0, result.totalOwedToYou, 0.01)
        assertEquals(0.0, result.totalYouOwe, 0.01)
        
        // Alice should have a balance of -50 (she owes 50)
        assertEquals(-50.0, result.balances["Alice"]!!, 0.01)
        // You should have a balance of +50 (owed 50)
        assertEquals(50.0, result.balances[currentUserId]!!, 0.01)
    }

    @Test
    fun `calculate 3-person equal split paid by You`() {
        // Scenario: You pay 300, split equally (You, Alice, Bob)
        // Result: Alice owes 100, Bob owes 100 -> You are owed 200
        val expenses = listOf(
            createExpense(id = "e1", amount = 300.0, paidBy = currentUserId)
        )
        val splits = listOf(
            createSplit(expenseId = "e1", personId = currentUserId, amount = 100.0),
            createSplit(expenseId = "e1", personId = "Alice", amount = 100.0),
            createSplit(expenseId = "e1", personId = "Bob", amount = 100.0)
        )
        val userIds = listOf(currentUserId, "Alice", "Bob")

        val result = useCase.execute(expenses, splits, emptyList(), userIds, currentUserId)

        assertEquals(200.0, result.totalOwedToYou, 0.01)
        
        // Check individual balances
        assertEquals(-100.0, result.balances["Alice"]!!, 0.01)
        assertEquals(-100.0, result.balances["Bob"]!!, 0.01)
    }

    @Test
    fun `calculate mixed payers`() {
        // Scenario: 
        // 1. You pay 100 for Lunch (You, Alice) -> Alice owes 50
        // 2. Alice pays 200 for Dinner (You, Alice) -> You owe 100
        // Net: You owe Alice 50
        
        val expenses = listOf(
            createExpense(id = "lunch", amount = 100.0, paidBy = currentUserId),
            createExpense(id = "dinner", amount = 200.0, paidBy = "Alice")
        )
        val splits = listOf(
            createSplit(expenseId = "lunch", personId = currentUserId, amount = 50.0),
            createSplit(expenseId = "lunch", personId = "Alice", amount = 50.0),
            createSplit(expenseId = "dinner", personId = "Alice", amount = 100.0),
            createSplit(expenseId = "dinner", personId = currentUserId, amount = 100.0)
        )
        val userIds = listOf(currentUserId, "Alice")

        val result = useCase.execute(expenses, splits, emptyList(), userIds, currentUserId)

        assertEquals(0.0, result.totalOwedToYou, 0.01)
        assertEquals(50.0, result.totalYouOwe, 0.01)
        
        // Balances
        assertEquals(50.0, result.balances["Alice"]!!, 0.01) // Alice is owed 50
        assertEquals(-50.0, result.balances[currentUserId]!!, 0.01) // You owe 50
    }

    @Test
    fun `debt simplification A owes B, B owes C`() {
        // Scenario (A->B->C):
        // Alice (A) owes Bob (B) 100
        // Bob (B) owes Charlie (C) 100
        // Simplified: Alice owes Charlie 100, Bob is clear.
        
        // 1. Bob pays 100 for Alice
        // 2. Charlie pays 100 for Bob
        
        val expenses = listOf(
            createExpense(id = "e1", amount = 100.0, paidBy = "Bob"),
            createExpense(id = "e2", amount = 100.0, paidBy = "Charlie")
        )
        val splits = listOf(
            // Expense 1: Bob pays 100 for Alice (Alice splits 100, Bob 0 in terms of consumption for this logic? No, usually 'pays for' implies split is 100% other)
            // Let's say Bob paid 100, split only for Alice.
            createSplit(expenseId = "e1", personId = "Alice", amount = 100.0), 
            
            // Expense 2: Charlie paid 100, split only for Bob.
            createSplit(expenseId = "e2", personId = "Bob", amount = 100.0)
        )
        
        val userIds = listOf("Alice", "Bob", "Charlie", currentUserId)
        
        // Execute with SIMPLIFY strategy
        val result = useCase.execute(expenses, splits, emptyList(), userIds, currentUserId, DebtStrategy.SIMPLIFY)
        
        // Balances:
        // Alice: -100 (Consumed 100, Paid 0)
        // Bob: +100 (Paid 100, Consumed 0) - 100 (Consumed 100 in e2 payment) = 0?
        // Wait, e2: Charlie paid 100 for Bob. Bob consumed 100. Bob's balance from e2 is -100.
        // Bob's total balance: +100 (from e1) - 100 (from e2) = 0.
        // Charlie: +100 (Paid 100, Consumed 0)
        
        assertEquals(-100.0, result.balances["Alice"]!!, 0.01)
        assertEquals(0.0, result.balances["Bob"]!!, 0.01)
        assertEquals(100.0, result.balances["Charlie"]!!, 0.01)
        
        // Transactions should show Alice -> Charlie 100
        val tx = result.transactions.find { it.fromId == "Alice" && it.toId == "Charlie" }
        assertTrue("Expected transaction Alice -> Charlie not found", tx != null)
        assertEquals(100.0, tx!!.amount, 0.01)
        
        // Ensure no transaction involves Bob
        val bobTx = result.transactions.any { it.fromId == "Bob" || it.toId == "Bob" }
        assertTrue("Bob should not be involved in simplified transactions", !bobTx)
    }

    @Test
    fun `settlement reduces debt`() {
        // Scenario: You pay 100 for Alice. Alice owes 100.
        // Then Alice pays you 100 (Settlement).
        // Net: 0.
        
        val expenses = listOf(
             createExpense(id = "e1", amount = 100.0, paidBy = currentUserId)
        )
        val splits = listOf(
            createSplit(expenseId = "e1", personId = "Alice", amount = 100.0)
        )
        val settlements = listOf(
            SettlementEntity(
                id = "s1",
                dashboardId = "d1",
                fromId = "Alice",
                toId = currentUserId,
                amount = 100.0,
                createdAt = 2000L
            )
        )
        val userIds = listOf(currentUserId, "Alice")
        
        val result = useCase.execute(expenses, splits, settlements, userIds, currentUserId)
        
        assertEquals(0.0, result.balances["Alice"]!!, 0.01)
        assertEquals(0.0, result.balances[currentUserId]!!, 0.01)
        assertEquals(0.0, result.totalOwedToYou, 0.01)
    }

    // Helpers
    private fun createExpense(id: String, amount: Double, paidBy: String): ExpenseEntity {
        return ExpenseEntity(
            id = id,
            dashboardId = "d1",
            description = "Test Expense",
            amount = amount,
            paidBy = paidBy,
            category = "food",
            createdAt = 1000L
        )
    }

    private fun createSplit(expenseId: String, personId: String, amount: Double): SplitEntity {
        return SplitEntity(
            id = "s_${expenseId}_$personId",
            expenseId = expenseId,
            personId = personId,
            amount = amount
        )
    }
}
