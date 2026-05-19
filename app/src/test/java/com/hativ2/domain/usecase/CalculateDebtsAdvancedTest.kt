package com.hativ2.domain.usecase

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.data.entity.SplitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CalculateDebtsAdvancedTest {

    private val useCase = CalculateDebtsUseCase()

    private val userA = "user_a"
    private val userB = "user_b"
    private val userC = "user_c"

    // -------- Balance invariant: sum of all balances must always equal 0 --------

    @Test
    fun `balance invariant holds for two-person single expense`() {
        val expId = "exp1"
        val expense = expense(expId, amount = 100.0, paidBy = userA)
        val splits = listOf(split(expId, userB, 100.0))

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            userIds = listOf(userA, userB),
            currentUserId = userA
        )

        assertBalanceSumIsZero(result.balances)
    }

    @Test
    fun `balance invariant holds for five-person multi-payer unequal splits`() {
        val users = (1..5).map { "user_$it" }
        val expenses = listOf(
            expense("e1", 300.0, users[0]),
            expense("e2", 150.0, users[1]),
            expense("e3", 420.0, users[2])
        )
        val splits = listOf(
            split("e1", users[0], 60.0), split("e1", users[1], 80.0),
            split("e1", users[2], 70.0), split("e1", users[3], 50.0), split("e1", users[4], 40.0),
            split("e2", users[1], 30.0), split("e2", users[2], 50.0), split("e2", users[3], 70.0),
            split("e3", users[0], 100.0), split("e3", users[2], 80.0), split("e3", users[3], 90.0),
            split("e3", users[4], 150.0)
        )

        val result = useCase.execute(
            expenses = expenses,
            splits = splits,
            userIds = users,
            currentUserId = users[0]
        )

        assertBalanceSumIsZero(result.balances)
    }

    @Test
    fun `balance invariant holds after partial settlements`() {
        val expId = "exp1"
        val expense = expense(expId, 300.0, userA)
        val splits = listOf(
            split(expId, userA, 100.0),
            split(expId, userB, 100.0),
            split(expId, userC, 100.0)
        )
        val settlements = listOf(
            SettlementEntity("s1", "dash1", userB, userA, 60.0, 0L)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            settlements = settlements,
            userIds = listOf(userA, userB, userC),
            currentUserId = userA
        )

        assertBalanceSumIsZero(result.balances)
    }

    @Test
    fun `balance invariant holds after full settlement`() {
        val expId = "exp1"
        val expense = expense(expId, 100.0, userA)
        val splits = listOf(split(expId, userB, 100.0))
        val settlements = listOf(
            SettlementEntity("s1", "dash1", userB, userA, 100.0, 0L)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            settlements = settlements,
            userIds = listOf(userA, userB),
            currentUserId = userA
        )

        assertBalanceSumIsZero(result.balances)
        assertEquals(0.0, result.balances[userA]!!, 0.01)
        assertEquals(0.0, result.balances[userB]!!, 0.01)
    }

    @Test
    fun `balance invariant holds with floating point edge amounts`() {
        val expId = "exp1"
        val expense = expense(expId, 100.0, userA)
        val splitAmount = 100.0 / 3.0
        val splits = listOf(
            split(expId, userA, splitAmount),
            split(expId, userB, splitAmount),
            split(expId, userC, splitAmount)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            userIds = listOf(userA, userB, userC),
            currentUserId = userA
        )

        assertBalanceSumIsZero(result.balances)
    }

    // -------- Overpayment: excess settlement flips debt direction --------

    @Test
    fun `overpayment_settlement_produces_reverse_debt`() {
        val expId = "exp1"
        val expense = expense(expId, 100.0, userA)
        val splits = listOf(split(expId, userB, 100.0))
        // B overpays: settles 150 when only 100 was owed
        val settlements = listOf(
            SettlementEntity("s1", "dash1", userB, userA, 150.0, 0L)
        )

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            settlements = settlements,
            userIds = listOf(userA, userB),
            currentUserId = userA
        )

        // B's balance: -100 + 150 = +50 (B is owed 50)
        // A's balance: +100 - 150 = -50 (A now owes 50)
        assertEquals(-50.0, result.balances[userA]!!, 0.01)
        assertEquals(50.0, result.balances[userB]!!, 0.01)
        assertBalanceSumIsZero(result.balances)
    }

    // -------- Multiple settlements from the same person --------

    @Test
    fun `multiple_settlements_from_same_person_accumulate_correctly`() {
        val expId = "exp1"
        val expense = expense(expId, 90.0, userA)
        val splits = listOf(split(expId, userB, 90.0))
        // Three partial settlements totalling 90
        val settlements = listOf(
            SettlementEntity("s1", "dash1", userB, userA, 30.0, 1L),
            SettlementEntity("s2", "dash1", userB, userA, 30.0, 2L),
            SettlementEntity("s3", "dash1", userB, userA, 30.0, 3L)
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
        assertTrue(result.transactions.isEmpty())
    }

    // -------- Circular debt: SIMPLIFY should reduce to 0 transactions --------

    @Test
    fun `circular_debt_simplifies_to_zero_transactions`() {
        // A pays B 10, B pays C 10, C pays A 10 → all net balances are 0
        val e1 = expense("e1", 10.0, userA)
        val e2 = expense("e2", 10.0, userB)
        val e3 = expense("e3", 10.0, userC)
        val splits = listOf(
            split("e1", userB, 10.0),
            split("e2", userC, 10.0),
            split("e3", userA, 10.0)
        )

        val result = useCase.execute(
            expenses = listOf(e1, e2, e3),
            splits = splits,
            userIds = listOf(userA, userB, userC),
            currentUserId = userA,
            strategy = DebtStrategy.SIMPLIFY
        )

        assertEquals(0.0, result.balances[userA]!!, 0.01)
        assertEquals(0.0, result.balances[userB]!!, 0.01)
        assertEquals(0.0, result.balances[userC]!!, 0.01)
        assertTrue("SIMPLIFY must produce 0 transactions for circular debts", result.transactions.isEmpty())
    }

    // -------- Large group debt invariant --------

    @Test
    fun `large_group_simplify_debt_invariant`() {
        val users = (1..10).map { "user_$it" }
        // 20 expenses with various payers and unequal splits
        val expenses = (1..20).map { i ->
            expense("e$i", (i * 37.5), users[i % users.size])
        }
        val splits = expenses.flatMap { exp ->
            // Split each expense across 4 people
            (0..3).map { j ->
                split(exp.id, users[(expenses.indexOf(exp) + j) % users.size], exp.amount / 4.0)
            }
        }

        val result = useCase.execute(
            expenses = expenses,
            splits = splits,
            userIds = users,
            currentUserId = users[0],
            strategy = DebtStrategy.SIMPLIFY
        )

        assertBalanceSumIsZero(result.balances)
    }

    // -------- Settlement commutativity --------

    @Test
    fun `settlement_order_does_not_affect_final_balances`() {
        val expId = "exp1"
        val expense = expense(expId, 300.0, userA)
        val splits = listOf(
            split(expId, userA, 100.0),
            split(expId, userB, 100.0),
            split(expId, userC, 100.0)
        )
        val settlement1 = SettlementEntity("s1", "dash1", userB, userA, 60.0, 1L)
        val settlement2 = SettlementEntity("s2", "dash1", userC, userA, 40.0, 2L)

        val resultForward = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            settlements = listOf(settlement1, settlement2),
            userIds = listOf(userA, userB, userC),
            currentUserId = userA
        )

        val resultReversed = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            settlements = listOf(settlement2, settlement1),
            userIds = listOf(userA, userB, userC),
            currentUserId = userA
        )

        assertEquals(resultForward.balances[userA]!!, resultReversed.balances[userA]!!, 0.001)
        assertEquals(resultForward.balances[userB]!!, resultReversed.balances[userB]!!, 0.001)
        assertEquals(resultForward.balances[userC]!!, resultReversed.balances[userC]!!, 0.001)
    }

    // -------- currentUserId not in userIds: no crash, empty personal totals --------

    @Test
    fun `currentUserId_not_in_userIds_is_safe`() {
        val expId = "exp1"
        val expense = expense(expId, 100.0, userA)
        val splits = listOf(split(expId, userB, 100.0))

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            userIds = listOf(userA, userB),
            currentUserId = "outsider_user"
        )

        assertEquals(0.0, result.totalOwedToYou, 0.01)
        assertEquals(0.0, result.totalYouOwe, 0.01)
        assertTrue(result.owedToYou.isEmpty())
        assertTrue(result.youOwe.isEmpty())
    }

    // -------- Zero-amount expense produces no transactions --------

    @Test
    fun `zero_amount_expense_produces_no_transactions`() {
        val expId = "exp1"
        val expense = expense(expId, 0.0, userA)
        val splits = listOf(split(expId, userA, 0.0), split(expId, userB, 0.0))

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            userIds = listOf(userA, userB),
            currentUserId = userA
        )

        assertTrue(result.transactions.isEmpty())
        assertEquals(0.0, result.totalOwedToYou, 0.001)
        assertEquals(0.0, result.totalYouOwe, 0.001)
    }

    // -------- Negative split amount: should not corrupt balances --------

    @Test
    fun `negative_split_amount_does_not_produce_phantom_debt`() {
        val expId = "exp1"
        val expense = expense(expId, 100.0, userA)
        // -10 is malformed input; the use case should still maintain balance integrity
        val splits = listOf(split(expId, userB, -10.0))

        val result = useCase.execute(
            expenses = listOf(expense),
            splits = splits,
            userIds = listOf(userA, userB),
            currentUserId = userA
        )

        // The balance sum invariant must still hold
        assertBalanceSumIsZero(result.balances)
    }

    // -------- Debt calculation performance --------

    @Test
    fun `one_hundred_expenses_twenty_people_simplify_completes_quickly`() {
        val users = (1..20).map { "user_$it" }
        val expenses = (1..100).map { i ->
            expense("e$i", (i * 10.0), users[i % users.size])
        }
        val splits = expenses.flatMap { exp ->
            (0..4).map { j ->
                split(exp.id, users[(expenses.indexOf(exp) + j) % users.size], exp.amount / 5.0)
            }
        }

        val start = System.currentTimeMillis()
        val result = useCase.execute(
            expenses = expenses,
            splits = splits,
            userIds = users,
            currentUserId = users[0],
            strategy = DebtStrategy.SIMPLIFY
        )
        val elapsed = System.currentTimeMillis() - start

        assertTrue("SIMPLIFY for 100 expenses / 20 people must complete in < 100ms (took ${elapsed}ms)", elapsed < 100)
        assertBalanceSumIsZero(result.balances)
    }

    @Test
    fun `one_thousand_expenses_fifty_people_simplify_completes_in_time`() {
        val users = (1..50).map { "user_$it" }
        val expenses = (1..1000).map { i ->
            expense("e$i", (i * 5.0), users[i % users.size])
        }
        val splits = expenses.flatMap { exp ->
            (0..2).map { j ->
                split(exp.id, users[(expenses.indexOf(exp) + j) % users.size], exp.amount / 3.0)
            }
        }

        val start = System.currentTimeMillis()
        useCase.execute(
            expenses = expenses,
            splits = splits,
            userIds = users,
            currentUserId = users[0],
            strategy = DebtStrategy.SIMPLIFY
        )
        val elapsed = System.currentTimeMillis() - start

        assertTrue("SIMPLIFY for 1000 expenses / 50 people must complete in < 500ms (took ${elapsed}ms)", elapsed < 500)
    }

    // -------- Duplicate expense should double debt, not deduplicate --------

    @Test
    fun `duplicate_expense_doubles_debt_not_deduplicated`() {
        val e1 = expense("e1", 100.0, userA)
        val e2 = expense("e2", 100.0, userA)  // Same amount, same payer, different ID
        val splits = listOf(
            split("e1", userB, 100.0),
            split("e2", userB, 100.0)
        )

        val result = useCase.execute(
            expenses = listOf(e1, e2),
            splits = splits,
            userIds = listOf(userA, userB),
            currentUserId = userA
        )

        assertEquals(200.0, result.balances[userA]!!, 0.01)
        assertEquals(-200.0, result.balances[userB]!!, 0.01)
    }

    // -------- Helpers --------

    private fun expense(id: String, amount: Double, paidBy: String) = ExpenseEntity(
        id = id,
        dashboardId = "dash1",
        description = "Expense $id",
        amount = amount,
        paidBy = paidBy,
        category = "Food",
        createdAt = 0L
    )

    private fun split(expenseId: String, personId: String, amount: Double) = SplitEntity(
        id = "${expenseId}_${personId}",
        expenseId = expenseId,
        personId = personId,
        amount = amount
    )

    private fun assertBalanceSumIsZero(balances: Map<String, Double>) {
        val sum = balances.values.sum()
        assertTrue(
            "Sum of all balances must be 0.0 (was $sum). Balances: $balances",
            abs(sum) < 0.02
        )
    }
}
