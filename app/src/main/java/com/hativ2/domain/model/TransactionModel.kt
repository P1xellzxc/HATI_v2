package com.hativ2.domain.model

data class TransactionModel(
    val fromId: String,
    val toId: String,
    val amount: Double
)
