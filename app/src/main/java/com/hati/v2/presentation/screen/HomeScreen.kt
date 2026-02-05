package com.hati.v2.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hati.v2.data.repository.TransactionRepository
import com.hati.v2.domain.model.Transaction
import com.hati.v2.presentation.animation.AntigravityEntrance
import com.hati.v2.presentation.animation.FallingLayout
import com.hati.v2.presentation.components.HalftoneOverlay
import com.hati.v2.presentation.components.MangaCard
import com.hati.v2.presentation.theme.MangaColors
import com.hati.v2.presentation.theme.MangaTypography
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeScreen - Main dashboard after login.
 * Displays transactions with Manga styling and Antigravity animations.
 */
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaColors.White)
    ) {
        // Background halftone pattern
        HalftoneOverlay(
            dotSize = 2f,
            spacing = 16f
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with Antigravity entrance
            AntigravityEntrance(delay = 0L) {
                MangaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = "HATI²",
                            style = MangaTypography.displaySmall.copy(color = MangaColors.Black)
                        )
                        
                        // Logout button
                        androidx.compose.foundation.clickable(onClick = {
                            scope.launch {
                                viewModel.logout()
                                onLogout()
                            }
                        }) {
                            MangaCard(
                                backgroundColor = MangaColors.Black,
                                shadowOffset = 2.dp
                            ) {
                                BasicText(
                                    text = "LOGOUT",
                                    style = MangaTypography.labelLarge.copy(color = MangaColors.White),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Section title
            FallingLayout(delay = 100L) {
                BasicText(
                    text = "TRANSACTIONS",
                    style = MangaTypography.headlineLarge.copy(color = MangaColors.Black)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Transaction list with staggered falling animation
            if (transactions.isEmpty()) {
                FallingLayout(delay = 200L) {
                    MangaCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = "NO TRANSACTIONS YET!",
                                style = MangaTypography.headlineSmall.copy(color = MangaColors.Black)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(transactions) { index, transaction ->
                        FallingLayout(delay = (index + 2) * 50L) {
                            TransactionCard(transaction = transaction)
                        }
                    }
                }
            }
        }
        
        // FAB with Antigravity entrance
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            AntigravityEntrance(delay = 300L) {
                androidx.compose.foundation.clickable(onClick = {
                    // TODO: Navigate to add transaction
                }) {
                    MangaCard(
                        backgroundColor = MangaColors.Black,
                        shadowOffset = 6.dp
                    ) {
                        BasicText(
                            text = "+",
                            style = MangaTypography.displayMedium.copy(color = MangaColors.White),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Transaction card with Manga styling.
 */
@Composable
fun TransactionCard(transaction: Transaction) {
    MangaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                BasicText(
                    text = transaction.description.uppercase(),
                    style = MangaTypography.headlineSmall.copy(color = MangaColors.Black)
                )
                BasicText(
                    text = transaction.category.uppercase(),
                    style = MangaTypography.labelMedium.copy(color = MangaColors.Black.copy(alpha = 0.6f))
                )
            }
            
            BasicText(
                text = "₱${String.format("%.2f", transaction.amount)}",
                style = MangaTypography.headlineMedium.copy(color = MangaColors.Black)
            )
        }
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions
    
    // TODO: Get actual group ID from navigation or user preferences
    private val currentGroupId = "demo-group"
    
    init {
        loadTransactions()
    }
    
    private fun loadTransactions() {
        viewModelScope.launch {
            transactionRepository.getTransactionsByGroup(currentGroupId)
                .collect { _transactions.value = it }
        }
    }
    
    suspend fun logout() {
        supabaseClient.auth.signOut()
    }
}
