package com.hati.v2.presentation.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hati.v2.data.local.DashboardEntity
import com.hati.v2.data.local.DashboardDao
import com.hati.v2.data.repository.DashboardRepository
import com.hati.v2.data.repository.TransactionRepository
import com.hati.v2.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val dashboardRepository: DashboardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dashboardId: String = checkNotNull(savedStateHandle["dashboardId"])

    private val _dashboard = MutableStateFlow<DashboardEntity?>(null)
    val dashboard: StateFlow<DashboardEntity?> = _dashboard

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _members = MutableStateFlow<List<com.hati.v2.data.local.MemberEntity>>(emptyList())
    val members: StateFlow<List<com.hati.v2.data.local.MemberEntity>> = _members

    val categoryStats: StateFlow<Map<String, Double>> = kotlinx.coroutines.flow.map(transactions) { list ->
        list.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        loadDashboard()
        loadTransactions()
        loadMembers()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _dashboard.value = dashboardRepository.getDashboardById(dashboardId)
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            transactionRepository.getTransactionsByGroup(dashboardId)
                .collect { _transactions.value = it }
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            dashboardRepository.getMembers(dashboardId)
                .collect { _members.value = it }
        }
    }

    fun addTransaction(description: String, amount: Double, category: String, paidBy: String) {
        viewModelScope.launch {
            val transaction = Transaction(
                id = java.util.UUID.randomUUID().toString(),
                groupId = dashboardId,
                description = description,
                amount = amount,
                category = category,
                paidBy = paidBy,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )
            transactionRepository.createTransaction(transaction)
        }
    }
}
