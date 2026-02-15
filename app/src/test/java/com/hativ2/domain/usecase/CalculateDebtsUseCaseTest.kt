package com.hativ2.domain.usecase

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SplitEntity
import com.hativ2.domain.model.DebtSummaryModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class CalculateDebtsUseCaseTest {

    private val useCase = CalculateDebtsUseCase()

    private val userA = "user_a"
    private val userB = "user_b"
    private val userC = "user_c"
    private val userIds = listOf(userA, userB, userC)

    @Test
    fun `equal split between 3 people`() {
        val expenseId = UUID.randomUUID().toString()
        val expense = ExpenseEntity(
            id = expenseId,
            dashboardId = "dash1",
            description = "Dinner",
            amount = 300.0,
            paidBy = userA,
            category = "Food",
            createdAt = System.currentTimeMillis()
        )

        val splits = listOf(
            SplitEntity(UUID.randomUUID().toString(), expenseId, userA, 100.0),
            SplitEntity(UUID.randomUUID().toString(), expenseId, userB, 100.0),
            SplitEntity(UUID.randomUUID().toString(), expenseId, userC, 100.0)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            userIds = userIds,
            currentUserId = userA
        )

        assertEquals(200.0, result.balances[userA]!!, 0.01)
        assertEquals(-100.0, result.balances[userB]!!, 0.01)
        assertEquals(-100.0, result.balances[userC]!!, 0.01)

        val transactions = result.transactions
        assertEquals(2, transactions.size)
        
        val bToA = transactions.find { it.fromId == userB && it.toId == userA }
        val cToA = transactions.find { it.fromId == userC && it.toId == userA }
        
        assertEquals(100.0, bToA?.amount ?: 0.0, 0.01)
        assertEquals(100.0, cToA?.amount ?: 0.0, 0.01)
    }

    @Test
    fun `uneven split`() {
        val expenseId = UUID.randomUUID().toString()
        val expense = ExpenseEntity(
            id = expenseId,
            dashboardId = "dash1",
            description = "Uber",
            amount = 100.0,
            paidBy = userA,
            category = "Transport",
            createdAt = System.currentTimeMillis()
        )

        val splits = listOf(
            SplitEntity(UUID.randomUUID().toString(), expenseId, userA, 10.0),
            SplitEntity(UUID.randomUUID().toString(), expenseId, userB, 30.0),
            SplitEntity(UUID.randomUUID().toString(), expenseId, userC, 60.0)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            userIds = userIds,
            currentUserId = userA
        )

        assertEquals(90.0, result.balances[userA]!!, 0.01)
        assertEquals(-30.0, result.balances[userB]!!, 0.01)
        assertEquals(-60.0, result.balances[userC]!!, 0.01)
    }
    
    @Test
    fun `A pays for B only`() {
        val expenseId = UUID.randomUUID().toString()
        val expense = ExpenseEntity(
            id = expenseId,
            dashboardId = "dash1",
            description = "Lunch",
            amount = 50.0,
            paidBy = userA,
            category = "Food",
            createdAt = System.currentTimeMillis()
        )

        val splits = listOf(
            SplitEntity(UUID.randomUUID().toString(), expenseId, userB, 50.0)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            userIds = userIds,
            currentUserId = userA
        )

        assertEquals(50.0, result.balances[userA]!!, 0.01)
        assertEquals(-50.0, result.balances[userB]!!, 0.01)
        assertEquals(0.0, result.balances[userC]!!, 0.01)
    }

    @Test
    fun `debt simplification A owes B and B owes C implies A owes C`() {
        val exp1Id = "exp1"
        val exp1 = ExpenseEntity(
            id = exp1Id, 
            dashboardId = "dash1", 
            description = "Lunch", 
            amount = 10.0, 
            paidBy = userB, 
            category = "Food", 
            createdAt = 0L
        )
        val split1 = listOf(SplitEntity("s1", exp1Id, userA, 10.0))

        val exp2Id = "exp2"
        val exp2 = ExpenseEntity(
            id = exp2Id, 
            dashboardId = "dash1", 
            description = "Drink", 
            amount = 10.0, 
            paidBy = userC, 
            category = "Drinks", 
            createdAt = 0L
        )
        val split2 = listOf(SplitEntity("s2", exp2Id, userB, 10.0))

        val result = useCase.execute(
            expenses = listOf(exp1, exp2),
            splits = split1 + split2,
            userIds = userIds,
            currentUserId = userA,
            strategy = DebtStrategy.SIMPLIFY
        )

        assertEquals(-10.0, result.balances[userA]!!, 0.01)
        assertEquals(0.0, result.balances[userB]!!, 0.01)
        assertEquals(10.0, result.balances[userC]!!, 0.01)

        val transactions = result.transactions
        assertEquals(1, transactions.size)
        assertEquals(userA, transactions[0].fromId)
        assertEquals(userC, transactions[0].toId)
        assertEquals(10.0, transactions[0].amount, 0.01)
    }
}
