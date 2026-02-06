# HATI v2 - Bug Fixes & Issues Resolution

## Critical Bugs 🔴

### Bug #1: Potential Null Pointer Exception in Navigation

**Location**: `MainActivity.kt` line 258

**Issue**:
```kotlin
if (startDestination == null) return

NavHost(
    navController = navController,
    startDestination = startDestination!! // ← Potential NPE
)
```

**Problem**: Using `!!` operator after null check creates a code smell. If `startDestination` becomes null between the check and usage, app crashes.

**Fix**:
```kotlin
@Composable
fun HatiNavigation(supabaseClient: SupabaseClient) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val session = supabaseClient.auth.currentSessionOrNull()
            startDestination = if (session != null) "home" else "login"
        } catch (e: Exception) {
            Timber.e(e, "Error determining start destination")
            startDestination = "login" // Fallback to login on error
        }
    }

    // Show loading state while determining start destination
    when (val destination = startDestination) {
        null -> LoadingScreen()
        else -> NavHost(
            navController = navController,
            startDestination = destination
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaColors.White),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MangaColors.Black)
    }
}
```

---

### Bug #2: Missing Transaction DAO Methods

**Location**: `TransactionDao.kt`

**Issue**: Repository references methods that don't exist in the DAO:
- `getTransactionById()`
- `markAsDeleted()`
- `getAllTransactions()`

**Fix**:
```kotlin
@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE groupId = :groupId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getTransactionsByGroup(groupId: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE groupId = :groupId AND isDeleted = 0")
    suspend fun getAllTransactions(groupId: String): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)
    
    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun markAsDeleted(id: String)
    
    @Query("DELETE FROM transactions WHERE isDeleted = 1 AND updatedAt < :timestamp")
    suspend fun deleteOldSoftDeletedTransactions(timestamp: Long)
}
```

---

### Bug #3: Missing User and Group DAO Interfaces

**Location**: Referenced in `HatiDatabase.kt` but not defined

**Issue**: Database class references DAOs that don't exist:
```kotlin
abstract fun userDao(): UserDao // ← Not defined
abstract fun groupDao(): GroupDao // ← Not defined
```

**Fix - Create UserDao.kt**:
```kotlin
package com.hati.v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)
    
    @Query("UPDATE users SET name = :name WHERE id = :userId")
    suspend fun updateName(userId: String, name: String)
    
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun delete(userId: String)
}
```

**Fix - Create GroupDao.kt**:
```kotlin
package com.hati.v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    
    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): GroupEntity?
    
    @Query("SELECT * FROM groups WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>
    
    @Query("SELECT * FROM groups WHERE id IN (:groupIds)")
    suspend fun getGroupsByIds(groupIds: List<String>): List<GroupEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>)
    
    @Query("UPDATE groups SET isDeleted = 1 WHERE id = :groupId")
    suspend fun markAsDeleted(groupId: String)
    
    @Query("DELETE FROM groups WHERE isDeleted = 1")
    suspend fun deleteSoftDeletedGroups()
}
```

---

### Bug #4: Missing Entity Definitions

**Location**: Entities referenced but not fully defined

**Fix - Create UserEntity.kt**:
```kotlin
package com.hati.v2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
```

**Fix - Create GroupEntity.kt**:
```kotlin
package com.hati.v2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "groups")
@TypeConverters(StringListConverter::class)
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val createdBy: String,
    val memberIds: List<String>, // Will need type converter
    val currency: String = "PHP",
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

// Type converter for List<String>
class StringListConverter {
    @TypeConverter
    fun fromString(value: String): List<String> {
        return value.split(",").filter { it.isNotBlank() }
    }
    
    @TypeConverter
    fun toString(list: List<String>): String {
        return list.joinToString(",")
    }
}
```

---

### Bug #5: Missing SyncMetadataDao

**Location**: Referenced in improved `TransactionRepository` but doesn't exist

**Fix - Create SyncMetadataEntity.kt**:
```kotlin
package com.hati.v2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val groupId: String,
    val lastSyncTime: Long,
    val lastSyncStatus: String, // "success", "failed", "in_progress"
    val pendingChanges: Int = 0
)
```

**Fix - Create SyncMetadataDao.kt**:
```kotlin
package com.hati.v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncMetadataDao {
    
    @Query("SELECT lastSyncTime FROM sync_metadata WHERE groupId = :groupId LIMIT 1")
    suspend fun getLastSyncTime(groupId: String): Long?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SyncMetadataEntity)
    
    @Query("UPDATE sync_metadata SET lastSyncTime = :timestamp, lastSyncStatus = 'success' WHERE groupId = :groupId")
    suspend fun updateLastSyncTime(groupId: String, timestamp: Long)
    
    @Query("UPDATE sync_metadata SET lastSyncStatus = :status WHERE groupId = :groupId")
    suspend fun updateSyncStatus(groupId: String, status: String)
    
    @Query("UPDATE sync_metadata SET pendingChanges = :count WHERE groupId = :groupId")
    suspend fun updatePendingChanges(groupId: String, count: Int)
}
```

**Update HatiDatabase.kt**:
```kotlin
@Database(
    entities = [
        TransactionEntity::class, 
        UserEntity::class, 
        GroupEntity::class,
        SyncMetadataEntity::class
    ], 
    version = 2, // Increment version
    exportSchema = true
)
@TypeConverters(StringListConverter::class)
abstract class HatiDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    
    companion object {
        const val DATABASE_NAME = "hati_database"
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add sync_metadata table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_metadata (
                        groupId TEXT PRIMARY KEY NOT NULL,
                        lastSyncTime INTEGER NOT NULL,
                        lastSyncStatus TEXT NOT NULL,
                        pendingChanges INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
    }
}
```

---

## High Priority Bugs 🟡

### Bug #6: No Error Handling in Login Flow

**Location**: `LoginScreen.kt`

**Issue**: Login button has no error handling

**Fix**:
```kotlin
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginState by viewModel.loginState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaColors.White)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AntigravityEntrance {
            MangaCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BasicText(
                        "HATI²",
                        style = MangaTypography.displayLarge.copy(color = MangaColors.Black)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Email field
                    MangaTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        enabled = loginState !is UiState.Loading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Password field
                    MangaTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = loginState !is UiState.Loading
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Error message
                    if (loginState is UiState.Error) {
                        BasicText(
                            (loginState as UiState.Error).message,
                            style = MangaTypography.bodyLarge.copy(color = Color.Red)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Login button
                    MangaButton(
                        onClick = {
                            viewModel.login(email, password)
                        },
                        text = when (loginState) {
                            is UiState.Loading -> "Logging in..."
                            else -> "Login"
                        },
                        enabled = loginState !is UiState.Loading &&
                                  email.isNotBlank() &&
                                  password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // Handle successful login
    LaunchedEffect(loginState) {
        if (loginState is UiState.Success) {
            onLoginSuccess()
        }
    }
}

// LoginViewModel needs implementation
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            
            try {
                // Validate input
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    _loginState.value = UiState.Error("Invalid email format")
                    return@launch
                }
                
                if (password.length < 6) {
                    _loginState.value = UiState.Error("Password must be at least 6 characters")
                    return@launch
                }
                
                // Attempt login
                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                _loginState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _loginState.value = UiState.Error(
                    message = when {
                        e.message?.contains("Invalid login credentials") == true ->
                            "Invalid email or password"
                        e.message?.contains("Email not confirmed") == true ->
                            "Please verify your email first"
                        e.message?.contains("network") == true ->
                            "Network error. Please check your connection."
                        else -> "Login failed. Please try again."
                    }
                )
                Timber.e(e, "Login failed")
            }
        }
    }
}
```

---

### Bug #7: Missing NetworkMonitor Implementation

**Location**: Referenced in `TransactionRepository` but doesn't exist

**Fix - Create NetworkMonitor.kt**:
```kotlin
package com.hati.v2.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

interface NetworkMonitor {
    fun isOnline(): Boolean
    fun observeNetworkState(): Flow<NetworkState>
}

sealed class NetworkState {
    object Available : NetworkState()
    object Unavailable : NetworkState()
}

@Singleton
class NetworkMonitorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkMonitor {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
        as ConnectivityManager
    
    override fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    override fun observeNetworkState(): Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkState.Available)
            }
            
            override fun onLost(network: Network) {
                trySend(NetworkState.Unavailable)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Send initial state
        trySend(if (isOnline()) NetworkState.Available else NetworkState.Unavailable)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

// Add to AppModule.kt
@Provides
@Singleton
fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
    return NetworkMonitorImpl(context)
}
```

---

### Bug #8: Incomplete HomeScreen Implementation

**Location**: `HomeScreen.kt` - Has placeholder comments

**Fix**:
```kotlin
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val groupInfo by viewModel.currentGroup.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(MangaColors.White)) {
        HalftoneOverlay()
        
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            AntigravityEntrance {
                MangaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
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
                                groupInfo?.name ?: "Loading...",
                                style = MangaTypography.headlineLarge
                            )
                            BasicText(
                                "Welcome, ${currentUser?.name ?: "User"}",
                                style = MangaTypography.bodyLarge
                            )
                        }
                        
                        MangaButton(
                            onClick = onLogout,
                            text = "Logout"
                        )
                    }
                }
            }
            
            // Transaction list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                itemsIndexed(transactions) { index, transaction ->
                    FallingLayout(delay = (index + 2) * 50L) {
                        TransactionCard(
                            transaction = transaction,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                
                if (transactions.isEmpty()) {
                    item {
                        EmptyState()
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // For FAB
                }
            }
        }
        
        // Floating Action Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            MangaButton(
                onClick = { viewModel.onAddTransactionClicked() },
                text = "+ Add"
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    MangaCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                BasicText(
                    transaction.description,
                    style = MangaTypography.headlineSmall
                )
                BasicText(
                    "Paid by ${transaction.paidBy}",
                    style = MangaTypography.labelMedium
                )
                BasicText(
                    transaction.category,
                    style = MangaTypography.labelMedium.copy(
                        color = MangaColors.Black.copy(alpha = 0.6f)
                    )
                )
            }
            
            BasicText(
                "₱${String.format("%.2f", transaction.amount)}",
                style = MangaTypography.headlineLarge
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                "No transactions yet",
                style = MangaTypography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            BasicText(
                "Tap + Add to create your first expense",
                style = MangaTypography.bodyLarge.copy(
                    color = MangaColors.Black.copy(alpha = 0.6f)
                )
            )
        }
    }
}
```

---

### Bug #9: Missing UI Components

**Location**: Components referenced but not defined

**Fix - Create MangaTextField.kt**:
```kotlin
package com.hati.v2.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hati.v2.presentation.theme.MangaColors
import com.hati.v2.presentation.theme.MangaTypography

@Composable
fun MangaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = modifier) {
        label?.let {
            androidx.compose.foundation.text.BasicText(
                text = it,
                style = MangaTypography.labelMedium.copy(color = MangaColors.Black),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = MangaColors.Black
                )
                .padding(12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                textStyle = MangaTypography.bodyLarge.copy(color = MangaColors.Black),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                cursorBrush = SolidColor(MangaColors.Black),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        androidx.compose.foundation.text.BasicText(
                            text = placeholder,
                            style = MangaTypography.bodyLarge.copy(
                                color = MangaColors.Black.copy(alpha = 0.4f)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
```

**Fix - Create MangaButton.kt**:
```kotlin
package com.hati.v2.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hati.v2.presentation.theme.MangaColors
import com.hati.v2.presentation.theme.MangaTypography

@Composable
fun MangaButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MangaColors.White,
    textColor: Color = MangaColors.Black
) {
    Box(
        modifier = modifier
            .mangaDropShadow(
                offset = 4.dp,
                color = if (enabled) MangaColors.Black else MangaColors.Black.copy(alpha = 0.3f)
            )
            .border(
                width = 3.dp,
                color = if (enabled) MangaColors.Black else MangaColors.Black.copy(alpha = 0.3f)
            )
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = MangaTypography.headlineSmall.copy(
                color = if (enabled) textColor else textColor.copy(alpha = 0.5f)
            )
        )
    }
}
```

---

## Medium Priority Bugs 🟢

### Bug #10: Missing ProGuard Rules
### Bug #11: No Crash Reporting
### Bug #12: Memory Leaks in Coroutines
### Bug #13: Inefficient Recomposition
### Bug #14: Missing Accessibility Support

*(See detailed fixes in separate document)*

---

## Testing Each Fix

For each bug fix, run:
```bash
# Unit tests
./gradlew testDebugUnitTest

# Integration tests
./gradlew connectedAndroidTest

# Manual testing checklist
```

---

## Regression Testing Checklist

- [ ] App launches successfully
- [ ] Login flow works
- [ ] Navigation works between screens
- [ ] Transactions display correctly
- [ ] Database operations succeed
- [ ] Sync works when online
- [ ] Offline mode works
- [ ] No crashes on rotation
- [ ] No memory leaks
