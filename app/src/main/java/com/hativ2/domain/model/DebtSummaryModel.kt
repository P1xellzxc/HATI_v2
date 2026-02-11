package com.hativ2.domain.model

data class DebtSummaryModel(
    val transactions: List<TransactionModel>,
    val balances: Map<String, Double>, // UserId -> Balance
    val totalOwedToYou: Double,
    val totalYouOwe: Double,
    val memberShares: Map<String, Double> = emptyMap(), // UserId -> Total Share (Spending)
    val owedToYou: List<TransactionModel> = emptyList(), // Transactions where current user is recipient
    val youOwe: List<TransactionModel> = emptyList()     // Transactions where current user is debtor
)
