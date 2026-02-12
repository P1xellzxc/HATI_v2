package com.hativ2.domain.usecase

import com.hativ2.data.entity.ExpenseEntity
import com.hativ2.data.entity.SettlementEntity
import com.hativ2.data.entity.SplitEntity
import com.hativ2.domain.model.DebtSummaryModel
import com.hativ2.domain.model.TransactionModel
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.round

enum class DebtStrategy {
    SIMPLIFY,   // Minimum transactions (greedy matching)
    PAIRWISE    // Direct who-owes-whom (mutual cancellation only)
}

class CalculateDebtsUseCase @Inject constructor() {

    fun execute(
        expenses: List<ExpenseEntity>,
        splits: List<SplitEntity>,
        settlements: List<SettlementEntity> = emptyList(),
        userIds: List<String>,
        currentUserId: String,
        strategy: DebtStrategy = DebtStrategy.SIMPLIFY
    ): DebtSummaryModel {
        val balances = calculateNetBalances(expenses, splits, settlements, userIds)
        
        val transactions = when (strategy) {
            DebtStrategy.SIMPLIFY -> simplifyDebts(balances)
            DebtStrategy.PAIRWISE -> calculatePairwiseDebts(expenses, splits, settlements)
        }

        var totalOwedToYou = 0.0
        var totalYouOwe = 0.0
        val owedToYou = mutableListOf<TransactionModel>()
        val youOwe = mutableListOf<TransactionModel>()

        for (tx in transactions) {
            if (tx.toId == currentUserId) {
                totalOwedToYou += tx.amount
                owedToYou.add(tx)
            }
            if (tx.fromId == currentUserId) {
                totalYouOwe += tx.amount
                youOwe.add(tx)
            }
        }

        val memberShares = calculateMemberShares(expenses = expenses, userIds = userIds)

        return DebtSummaryModel(
            transactions = transactions,
            balances = balances,
            totalOwedToYou = roundTwoDecimals(totalOwedToYou),
            totalYouOwe = roundTwoDecimals(totalYouOwe),
            memberShares = memberShares,
            owedToYou = owedToYou,
            youOwe = youOwe
        )
    }

    private fun calculateMemberShares(
        expenses: List<ExpenseEntity>,
        userIds: List<String>
    ): Map<String, Double> {
        // Calculate how much each person has PAID (not their split share)
        val paid = userIds.associateWith { 0.0 }.toMutableMap()
        for (expense in expenses) {
            val payerId = expense.paidBy ?: continue
            val current = paid[payerId] ?: 0.0
            paid[payerId] = current + expense.amount
        }
        return paid.mapValues { roundTwoDecimals(it.value) }
    }

    private fun calculateNetBalances(
        expenses: List<ExpenseEntity>,
        allSplits: List<SplitEntity>,
        settlements: List<SettlementEntity>,
        userIds: List<String>
    ): Map<String, Double> {
        val balances = userIds.associateWith { 0.0 }.toMutableMap()

        // Process expenses
        for (expense in expenses) {
            val payerId = expense.paidBy ?: continue
            val payerBalance = balances[payerId] ?: 0.0
            balances[payerId] = payerBalance + expense.amount

            val expenseSplits = allSplits.filter { it.expenseId == expense.id }
            
            for (split in expenseSplits) {
                val debtorId = split.personId
                val amount = split.amount
                val debtorBalance = balances[debtorId] ?: 0.0
                balances[debtorId] = debtorBalance - amount
            }
        }

        // Process settlements (from pays to, so from's balance goes down, to's goes up)
        for (settlement in settlements) {
            val fromBalance = balances[settlement.fromId] ?: 0.0
            val toBalance = balances[settlement.toId] ?: 0.0
            balances[settlement.fromId] = fromBalance + settlement.amount  // Payer gets credit
            balances[settlement.toId] = toBalance - settlement.amount      // Recipient's credit reduces
        }

        return balances
    }

    /**
     * Pairwise debts: Direct who-owes-whom with mutual cancellation only.
     * Does NOT simplify across 3+ people.
     */
    private fun calculatePairwiseDebts(
        expenses: List<ExpenseEntity>,
        allSplits: List<SplitEntity>,
        settlements: List<SettlementEntity>
    ): List<TransactionModel> {
        // Map<From, Map<To, Amount>>
        val debtMap = mutableMapOf<String, MutableMap<String, Double>>()

        // Accumulate debts from expenses
        for (expense in expenses) {
            val payer = expense.paidBy ?: continue
            val expenseSplits = allSplits.filter { it.expenseId == expense.id }
            
            for (split in expenseSplits) {
                if (split.personId == payer) continue  // Don't owe yourself
                if (split.amount <= 0) continue

                val debtorMap = debtMap.getOrPut(split.personId) { mutableMapOf() }
                val currentDebt = debtorMap[payer] ?: 0.0
                debtorMap[payer] = currentDebt + split.amount
            }
        }

        // Subtract settlements
        for (settlement in settlements) {
            val debtorMap = debtMap[settlement.fromId]
            if (debtorMap != null) {
                val currentDebt = debtorMap[settlement.toId] ?: 0.0
                debtorMap[settlement.toId] = currentDebt - settlement.amount
            }
        }

        val transactions = mutableListOf<TransactionModel>()
        val processedPairs = mutableSetOf<String>()

        // Pairwise cancellation
        debtMap.forEach { (debtor, targets) ->
            targets.forEach { (creditor, amount) ->
                val pairKey = listOf(debtor, creditor).sorted().joinToString(":")
                if (pairKey in processedPairs) return@forEach

                val reverseDebt = debtMap[creditor]?.get(debtor) ?: 0.0
                val netDebt = amount - reverseDebt
                val roundedNet = roundTwoDecimals(netDebt)

                if (roundedNet > 0.01) {
                    transactions.add(TransactionModel(debtor, creditor, roundedNet))
                } else if (roundedNet < -0.01) {
                    transactions.add(TransactionModel(creditor, debtor, abs(roundedNet)))
                }

                processedPairs.add(pairKey)
            }
        }

        return transactions
    }

    private fun simplifyDebts(balances: Map<String, Double>): List<TransactionModel> {
        val transactions = mutableListOf<TransactionModel>()

        data class Debtor(val id: String, var amount: Double)

        val givers = mutableListOf<Debtor>()
        val receivers = mutableListOf<Debtor>()

        balances.forEach { (id, amount) ->
            val rounded = roundTwoDecimals(amount)
            if (rounded < -0.01) {
                givers.add(Debtor(id, abs(rounded)))
            } else if (rounded > 0.01) {
                receivers.add(Debtor(id, rounded))
            }
        }

        givers.sortByDescending { it.amount }
        receivers.sortByDescending { it.amount }

        var i = 0
        var j = 0

        while (i < givers.size && j < receivers.size) {
            val giver = givers[i]
            val receiver = receivers[j]

            val transferAmount = minOf(giver.amount, receiver.amount)
            val roundedTransfer = roundTwoDecimals(transferAmount)

            if (roundedTransfer > 0.01) {
                transactions.add(TransactionModel(giver.id, receiver.id, roundedTransfer))
            }

            giver.amount -= transferAmount
            receiver.amount -= transferAmount

            if (giver.amount < 0.01) i++
            if (receiver.amount < 0.01) j++
        }

        return transactions
    }

    private fun roundTwoDecimals(value: Double): Double {
        return round(value * 100) / 100.0
    }
}
