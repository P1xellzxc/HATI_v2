# HATI v2 - Refactoring Guide

## 🎯 Refactoring Goals

1. **Improve Maintainability** - Cleaner, more readable code
2. **Enhance Testability** - Better separation of concerns
3. **Boost Performance** - Optimize critical paths
4. **Reduce Complexity** - Simplify complex logic
5. **Follow Best Practices** - Kotlin & Android standards

---

## 1. Extract UseCase Layer

### Current Problem
ViewModels directly calling repositories mixes business logic with presentation.

### Refactoring

**Before**:
```kotlin
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    fun loadTransactions(groupId: String) {
        viewModelScope.launch {
            transactionRepository.getTransactionsByGroup(groupId)
                .collect { /* ... */ }
        }
    }
}
```

**After**:
```kotlin
// domain/usecase/GetTransactionsByGroupUseCase.kt
class GetTransactionsByGroupUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(groupId: String): Flow<Result<List<Transaction>>> {
        return transactionRepository.getTransactionsByGroup(groupId)
            .map { transactions ->
                // Business logic here
                Result.success(transactions.sortedByDescending { it.createdAt })
            }
            .catch { error ->
                emit(Result.failure(error))
            }
    }
}

// presentation/viewmodel/HomeViewModel.kt
class HomeViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsByGroupUseCase
) : ViewModel() {
    
    fun loadTransactions(groupId: String) {
        viewModelScope.launch {
            getTransactionsUseCase(groupId)
                .collect { result -> /* Update UI state */ }
        }
    }
}
```

**Benefits**:
- Testable business logic
- Reusable across ViewModels
- Single Responsibility Principle

---

## 2. Improve State Management

### Current Problem
Multiple mutable states scattered across ViewModels.

### Refactoring

**Before**:
```kotlin
class HomeViewModel : ViewModel() {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    
    val transactions = _transactions.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val error = _error.asStateFlow()
}
```

**After**:
```kotlin
sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val transactions: List<Transaction>,
        val totalAmount: Double,
        val userBalance: Map<String, Double>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun loadTransactions(groupId: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            getTransactionsUseCase(groupId)
                .collect { result ->
                    _uiState.value = when {
                        result.isSuccess -> {
                            val transactions = result.getOrNull()!!
                            HomeUiState.Success(
                                transactions = transactions,
                                totalAmount = transactions.sumOf { it.amount },
                                userBalance = calculateBalances(transactions)
                            )
                        }
                        else -> HomeUiState.Error(
                            result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    }
                }
        }
    }
}
```

**Benefits**:
- Single source of truth
- Impossible states become unrepresentable
- Easier to test and debug

---

## 3. Extract Mappers

### Current Problem
Entity-to-Domain mapping scattered across repositories.

### Refactoring

**Before**:
```kotlin
class TransactionRepository {
    fun getTransactions() = transactionDao.getAll().map { entities ->
        entities.map { entity ->
            Transaction(
                id = entity.id,
                groupId = entity.groupId,
                // ... 10 more fields
            )
        }
    }
}
```

**After**:
```kotlin
// data/mapper/TransactionMapper.kt
object TransactionMapper {
    
    fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id,
            groupId = groupId,
            description = description,
            amount = amount,
            paidBy = paidBy,
            category = category,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt)
        )
    }
    
    fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            groupId = groupId,
            description = description,
            amount = amount,
            paidBy = paidBy,
            category = category,
            createdAt = createdAt.toEpochMilliseconds(),
            updatedAt = updatedAt.toEpochMilliseconds(),
            isSynced = false,
            isDeleted = false
        )
    }
}

// Usage in repository
class TransactionRepository {
    fun getTransactions() = transactionDao.getAll()
        .map { entities -> 
            entities.map { it.toDomain() }
        }
}
```

**Benefits**:
- Centralized mapping logic
- Easier to maintain
- Testable transformations

---

## 4. Introduce Result Wrapper

### Current Problem
Inconsistent error handling across layers.

### Refactoring

**Create sealed Result class**:
```kotlin
// domain/model/Result.kt
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(
        val exception: Exception,
        val message: String = exception.message ?: "Unknown error"
    ) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Extension functions
fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

fun <T> Result<T>.onError(action: (Exception) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}
```

**Usage**:
```kotlin
class TransactionRepository {
    suspend fun addTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            transactionDao.insert(transaction.toEntity())
            Result.Success(transaction)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

// In ViewModel
viewModel.addTransaction(transaction)
    .onSuccess { showSuccess() }
    .onError { showError(it.message) }
```

---

## 5. Optimize Database Queries

### Current Problem
Loading all transactions at once, inefficient for large datasets.

### Refactoring

**Before**:
```kotlin
@Query("SELECT * FROM transactions WHERE groupId = :groupId")
fun getTransactionsByGroup(groupId: String): Flow<List<TransactionEntity>>
```

**After - Pagination**:
```kotlin
@Query("""
    SELECT * FROM transactions 
    WHERE groupId = :groupId AND isDeleted = 0
    ORDER BY createdAt DESC
    LIMIT :limit OFFSET :offset
""")
suspend fun getTransactionsPaged(
    groupId: String, 
    limit: Int, 
    offset: Int
): List<TransactionEntity>

// Or use Paging 3
@Query("SELECT * FROM transactions WHERE groupId = :groupId")
fun getTransactionsPaged(groupId: String): PagingSource<Int, TransactionEntity>
```

**Indexed Queries**:
```kotlin
// Add indices to database
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["createdAt"]),
        Index(value = ["paidBy"])
    ]
)
data class TransactionEntity(...)
```

---

## 6. Extract Constants

### Current Problem
Magic numbers and strings scattered throughout code.

### Refactoring

**Before**:
```kotlin
FallingLayout(delay = (index + 2) * 50L)
MangaCard(borderWidth = 4.dp, shadowOffset = 4.dp)
```

**After**:
```kotlin
// presentation/theme/Dimensions.kt
object Dimensions {
    val BorderWidth = 4.dp
    val ShadowOffset = 4.dp
    val CardPadding = 16.dp
    val ScreenPadding = 24.dp
}

// presentation/animation/AnimationConstants.kt
object AnimationConstants {
    const val BASE_DELAY_MS = 50L
    const val STAGGER_MULTIPLIER = 2
    const val BOUNCE_DAMPNESS = 0.7f
}

// Usage
FallingLayout(
    delay = (index + AnimationConstants.STAGGER_MULTIPLIER) * 
            AnimationConstants.BASE_DELAY_MS
)

MangaCard(
    borderWidth = Dimensions.BorderWidth,
    shadowOffset = Dimensions.ShadowOffset
)
```

---

## 7. Refactor Composables

### Current Problem
Large composables with mixed concerns.

### Refactoring

**Before** (100+ lines):
```kotlin
@Composable
fun HomeScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Header code
        // Transaction list code  
        // FAB code
        // Dialog code
        // Loading indicator
        // Error handling
    }
}
```

**After** (extracted):
```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = { HomeTopBar(onLogout = viewModel::logout) },
        floatingActionButton = { AddTransactionFab(onClick = viewModel::showAddDialog) }
    ) { paddingValues ->
        HomeContent(
            uiState = uiState,
            onRetry = viewModel::loadTransactions,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is HomeUiState.Loading -> LoadingState()
        is HomeUiState.Success -> SuccessState(uiState.transactions)
        is HomeUiState.Error -> ErrorState(uiState.message, onRetry)
    }
}

@Composable
private fun HomeTopBar(onLogout: () -> Unit) { /* ... */ }

@Composable
private fun AddTransactionFab(onClick: () -> Unit) { /* ... */ }

@Composable
private fun SuccessState(transactions: List<Transaction>) {
    LazyColumn {
        items(transactions) { transaction ->
            TransactionCard(transaction)
        }
    }
}
```

**Benefits**:
- Smaller, focused functions
- Easier to test
- Better recomposition performance

---

## 8. Improve Error Messages

### Current Problem
Generic error messages don't help users.

### Refactoring

**Before**:
```kotlin
catch (e: Exception) {
    _error.value = "Error: ${e.message}"
}
```

**After**:
```kotlin
// domain/model/AppError.kt
sealed class AppError(val message: String, val userMessage: String) {
    class NetworkError : AppError(
        message = "Network request failed",
        userMessage = "No internet connection. Please check your network."
    )
    
    class DatabaseError : AppError(
        message = "Database operation failed",
        userMessage = "Failed to save data. Please try again."
    )
    
    class AuthError : AppError(
        message = "Authentication failed",
        userMessage = "Invalid credentials. Please check your email and password."
    )
    
    class ValidationError(field: String) : AppError(
        message = "Validation failed for $field",
        userMessage = "Please check $field and try again."
    )
}

// Usage
catch (e: Exception) {
    val appError = when (e) {
        is IOException -> AppError.NetworkError()
        is SQLiteException -> AppError.DatabaseError()
        is AuthException -> AppError.AuthError()
        else -> AppError.Unknown(e.message)
    }
    
    _error.value = appError.userMessage
    Timber.e(e, appError.message)
}
```

---

## 9. Add Extension Functions

### Refactoring Common Operations

```kotlin
// util/Extensions.kt

// Formatting
fun Double.toDisplayAmount(): String = "₱${String.format("%.2f", this)}"

fun Instant.toDisplayDate(): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        .format(Date(toEpochMilliseconds()))
}

// Validation
fun String.isValidEmail(): Boolean = 
    Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun Double.isValidAmount(): Boolean = 
    this > 0.0 && this < 1_000_000.0

// Collections
fun <T> List<T>.chunkedByDate(
    dateSelector: (T) -> Instant
): Map<String, List<T>> {
    return groupBy { item ->
        dateSelector(item).toDisplayDate()
    }
}

// Flow
fun <T> Flow<T>.throttleFirst(windowDuration: Long): Flow<T> = flow {
    var lastEmissionTime = 0L
    collect { value ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmissionTime >= windowDuration) {
            lastEmissionTime = currentTime
            emit(value)
        }
    }
}

// Usage
BasicText(transaction.amount.toDisplayAmount())
BasicText(transaction.createdAt.toDisplayDate())
```

---

## 10. Dependency Injection Improvements

### Refactoring

**Before** - Everything in one module:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // 20+ provides functions
}
```

**After** - Split by concern:
```kotlin
// di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HatiDatabase {
        return Room.databaseBuilder(
            context,
            HatiDatabase::class.java,
            HatiDatabase.DATABASE_NAME
        )
        .addMigrations(HatiDatabase.MIGRATION_1_2)
        .build()
    }
    
    @Provides
    fun provideTransactionDao(db: HatiDatabase) = db.transactionDao()
    
    @Provides
    fun provideUserDao(db: HatiDatabase) = db.userDao()
}

// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseClientFactory.create()
    
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitorImpl(context)
    }
}

// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideTransactionRepository(
        database: HatiDatabase,
        supabaseClient: SupabaseClient,
        networkMonitor: NetworkMonitor
    ): TransactionRepository = TransactionRepositoryImpl(
        database, supabaseClient, networkMonitor
    )
}
```

---

## Refactoring Checklist

### Before Refactoring
- [ ] Write tests for existing behavior
- [ ] Document current functionality
- [ ] Create feature branch
- [ ] Backup current code

### During Refactoring
- [ ] Make small, incremental changes
- [ ] Run tests after each change
- [ ] Commit frequently with clear messages
- [ ] Keep app functional at all times

### After Refactoring
- [ ] All tests passing
- [ ] Code review completed
- [ ] Performance verified
- [ ] Documentation updated
- [ ] Merge to main

---

## Priority Order

1. **High Impact, Low Effort**
   - Extract constants
   - Add extension functions
   - Improve error messages

2. **High Impact, Medium Effort**
   - Extract UseCase layer
   - Improve state management
   - Add Result wrapper

3. **High Impact, High Effort**
   - Database optimization
   - Pagination implementation
   - Comprehensive testing

4. **Medium Impact**
   - Extract mappers
   - Split DI modules
   - Refactor large composables

---

## Metrics to Track

- **Code Coverage**: Target 80%+
- **Method Complexity**: Max 15 cyclomatic complexity
- **File Length**: Max 500 lines per file
- **Function Length**: Max 50 lines per function
- **Build Time**: Track and optimize
- **App Size**: Monitor APK size

---

## Tools

- **Android Studio Refactoring Tools**: Extract Method, Rename, etc.
- **Detekt**: Static analysis for Kotlin
- **ktlint**: Code formatting
- **SonarQube**: Code quality metrics

```gradle
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

detekt {
    config = files("$projectDir/detekt.yml")
    buildUponDefaultConfig = true
}
```

---

**Remember**: Refactor incrementally. Don't try to do everything at once!
