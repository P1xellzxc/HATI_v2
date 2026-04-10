package com.hativ2.domain.usecase

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.data.entity.SplitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CalculateDebtsUseCaseTest {

    private val useCase = CalculateDebtsUseCase()

    private val userA = "user_a"
    private val userB = "user_b"
    private val userC = "user_c"
    private val userIds = listOf(userA, userB, userC)

    // -------- SIMPLIFY strategy tests --------

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

    // -------- PAIRWISE strategy tests --------

    @Test
    fun `pairwise strategy keeps direct debts`() {
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
            strategy = DebtStrategy.PAIRWISE
        )

        // Pairwise does NOT simplify across 3 people, so we expect 2 transactions
        assertEquals(2, result.transactions.size)
        val aToB = result.transactions.find { it.fromId == userA && it.toId == userB }
        val bToC = result.transactions.find { it.fromId == userB && it.toId == userC }
        assertEquals(10.0, aToB?.amount ?: 0.0, 0.01)
        assertEquals(10.0, bToC?.amount ?: 0.0, 0.01)
    }

    @Test
    fun `pairwise strategy cancels mutual debts`() {
        // A pays for B: 30, B pays for A: 10 → net A→B should be 0, B→A should be 20
        val exp1Id = "exp1"
        val exp1 = ExpenseEntity(exp1Id, "dash1", "Lunch", 30.0, userA, "Food", 0L)
        val split1 = listOf(SplitEntity("s1", exp1Id, userB, 30.0))

        val exp2Id = "exp2"
        val exp2 = ExpenseEntity(exp2Id, "dash1", "Drink", 10.0, userB, "Drinks", 0L)
        val split2 = listOf(SplitEntity("s2", exp2Id, userA, 10.0))

        val result = useCase.execute(
            expenses = listOf(exp1, exp2),
            splits = split1 + split2,
            userIds = listOf(userA, userB),
            currentUserId = userA,
            strategy = DebtStrategy.PAIRWISE
        )

        // Net: B owes A 20 (30 - 10)
        assertEquals(1, result.transactions.size)
        assertEquals(userB, result.transactions[0].fromId)
        assertEquals(userA, result.transactions[0].toId)
        assertEquals(20.0, result.transactions[0].amount, 0.01)
    }

    // -------- Settlement tests --------

    @Test
    fun `settlements reduce debt balances`() {
        val expenseId = "exp1"
        val expense = ExpenseEntity(expenseId, "dash1", "Dinner", 100.0, userA, "Food", 0L)
        val splits = listOf(
            SplitEntity("s1", expenseId, userA, 50.0),
            SplitEntity("s2", expenseId, userB, 50.0)
        )
        // B settles 30 to A
        val settlements = listOf(
            SettlementEntity("set-1", "dash1", userB, userA, 30.0, 0L)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            settlements = settlements,
            userIds = listOf(userA, userB),
            currentUserId = userA
        )

        // Before settlement: A = +50, B = -50
        // After settlement: B paid 30 to A → B's balance += 30 → -20, A's balance -= 30 → +20
        assertEquals(20.0, result.balances[userA]!!, 0.01)
        assertEquals(-20.0, result.balances[userB]!!, 0.01)
    }

    @Test
    fun `full settlement zeroes out debt`() {
        val expenseId = "exp1"
        val expense = ExpenseEntity(expenseId, "dash1", "Dinner", 100.0, userA, "Food", 0L)
        val splits = listOf(
            SplitEntity("s1", expenseId, userB, 100.0)
        )
        // B settles full 100 to A
        val settlements = listOf(
            SettlementEntity("set-1", "dash1", userB, userA, 100.0, 0L)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            settlements = settlements,
            userIds = listOf(userA, userB),
            currentUserId = userA
        )

        assertEquals(0.0, result.balances[userA]!!, 0.01)
        assertEquals(0.0, result.balances[userB]!!, 0.01)
        // No transactions needed when fully settled
        assertTrue(result.transactions.isEmpty())
    }

    // -------- owedToYou / youOwe classification tests --------

    @Test
    fun `owedToYou contains transactions where current user receives`() {
        val expenseId = "exp1"
        val expense = ExpenseEntity(expenseId, "dash1", "Dinner", 300.0, userA, "Food", 0L)
        val splits = listOf(
            SplitEntity("s1", expenseId, userA, 100.0),
            SplitEntity("s2", expenseId, userB, 100.0),
            SplitEntity("s3", expenseId, userC, 100.0)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            userIds = userIds,
            currentUserId = userA
        )

        assertEquals(2, result.owedToYou.size)
        assertEquals(200.0, result.totalOwedToYou, 0.01)
        assertEquals(0.0, result.totalYouOwe, 0.01)
        assertTrue(result.youOwe.isEmpty())
        assertTrue(result.owedToYou.all { it.toId == userA })
    }

    @Test
    fun `youOwe contains transactions where current user pays`() {
        val expenseId = "exp1"
        val expense = ExpenseEntity(expenseId, "dash1", "Dinner", 100.0, userB, "Food", 0L)
        val splits = listOf(
            SplitEntity("s1", expenseId, userA, 100.0)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            userIds = listOf(userA, userB),
            currentUserId = userA
        )

        assertEquals(1, result.youOwe.size)
        assertEquals(100.0, result.totalYouOwe, 0.01)
        assertEquals(0.0, result.totalOwedToYou, 0.01)
        assertTrue(result.owedToYou.isEmpty())
        assertTrue(result.youOwe.all { it.fromId == userA })
    }

    // -------- memberShares tests --------

    @Test
    fun `memberShares reflects how much each person paid`() {
        val exp1 = ExpenseEntity("exp1", "dash1", "Lunch", 100.0, userA, "Food", 0L)
        val exp2 = ExpenseEntity("exp2", "dash1", "Dinner", 200.0, userB, "Food", 0L)
        val splits = listOf(
            SplitEntity("s1", "exp1", userA, 50.0),
            SplitEntity("s2", "exp1", userB, 50.0),
            SplitEntity("s3", "exp2", userA, 100.0),
            SplitEntity("s4", "exp2", userB, 100.0)
        )

        val result = useCase.execute(
            expenses = listOf(exp1, exp2),
            splits = splits,
            userIds = listOf(userA, userB, userC),
            currentUserId = userA
        )

        assertEquals(100.0, result.memberShares[userA]!!, 0.01)
        assertEquals(200.0, result.memberShares[userB]!!, 0.01)
        assertEquals(0.0, result.memberShares[userC]!!, 0.01)
    }

    // -------- Edge cases --------

    @Test
    fun `no expenses produces zero balances and no transactions`() {
        val result = useCase.execute(
            expenses = emptyList(),
            splits = emptyList(),
            userIds = userIds,
            currentUserId = userA
        )

        assertEquals(0.0, result.balances[userA]!!, 0.01)
        assertEquals(0.0, result.balances[userB]!!, 0.01)
        assertEquals(0.0, result.balances[userC]!!, 0.01)
        assertTrue(result.transactions.isEmpty())
        assertEquals(0.0, result.totalOwedToYou, 0.01)
        assertEquals(0.0, result.totalYouOwe, 0.01)
    }

    @Test
    fun `floating point rounding does not produce tiny residual debts`() {
        // 100 / 3 = 33.333... — should not produce residual transactions after rounding
        val expenseId = "exp1"
        val expense = ExpenseEntity(expenseId, "dash1", "Split3", 100.0, userA, "Food", 0L)
        val splitAmount = 100.0 / 3.0
        val splits = listOf(
            SplitEntity("s1", expenseId, userA, splitAmount),
            SplitEntity("s2", expenseId, userB, splitAmount),
            SplitEntity("s3", expenseId, userC, splitAmount)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            userIds = userIds,
            currentUserId = userA
        )

        // All transaction amounts should be rounded to 2 decimal places
        result.transactions.forEach { tx ->
            val rounded = Math.round(tx.amount * 100) / 100.0
            assertEquals(rounded, tx.amount, 0.001)
        }
    }
}
