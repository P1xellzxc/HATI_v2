package com.hati.v2.data.remote

import com.hati.v2.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Supabase client configuration for HATI_v2.
 * Provides Auth, PostgREST, and Realtime plugins.
 * 
 * SECURITY: Never log sensitive financial data to Logcat.
 */
object SupabaseClientFactory {
    
    fun create(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // Auth plugin for user authentication
            install(Auth) {
                // Auto-refresh tokens
                alwaysAutoRefresh = true
            }
            
            // PostgREST plugin for database operations
            install(Postgrest)
            
            // Realtime plugin for live updates
            install(Realtime)
        }
    }
}

/**
 * DTO for expenses from Supabase.
 * Maps to the 'expenses' table.
 */
@kotlinx.serialization.Serializable
data class ExpenseDto(
    val id: String,
    val group_id: String,
    val description: String,
    val amount: Double,
    val paid_by: String,
    val category: String,
    val created_at: String,
    val updated_at: String
)

/**
 * DTO for users from Supabase.
 */
@kotlinx.serialization.Serializable
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val avatar_url: String? = null,
    val created_at: String
)

/**
 * DTO for groups from Supabase.
 */
@kotlinx.serialization.Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val created_by: String? = null,
    val created_at: String
)
