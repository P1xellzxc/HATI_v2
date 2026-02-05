package com.hati.v2.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hati.v2.data.local.DashboardEntity
import com.hati.v2.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HubViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _dashboards = MutableStateFlow<List<DashboardEntity>>(emptyList())
    val dashboards: StateFlow<List<DashboardEntity>> = _dashboards

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail

    init {
        loadDashboards()
        loadUserProfile()
    }

    private fun loadDashboards() {
        viewModelScope.launch {
            dashboardRepository.getAllDashboards()
                .collect { _dashboards.value = it }
        }
    }

    private fun loadUserProfile() {
        val user = supabaseClient.auth.currentUserOrNull()
        _userEmail.value = user?.email
    }

    fun createDashboard(name: String) {
        viewModelScope.launch {
            val user = supabaseClient.auth.currentUserOrNull()
            if (user != null) {
                dashboardRepository.createDashboard(name, user.id)
            }
        }
    }

    suspend fun logout() {
        supabaseClient.auth.signOut()
    }
}
