# HATI v2 - Complete Documentation Package

## 📋 Table of Contents
1. [README](#readme)
2. [Architecture Overview](#architecture)
3. [API Documentation](#api-docs)
4. [Setup Guide](#setup)
5. [Contributing Guide](#contributing)

---

# README

## HATI² - Expense Splitting Made Simple

<div align="center">
  <h3>🎌 Manga-Themed Expense Tracker 🎌</h3>
  <p>Split expenses with friends using beautiful anime-inspired design</p>
</div>

### ✨ Features

- 📱 **Clean Manga UI** - Bold lines, dramatic animations
- 🌐 **Offline-First** - Works without internet
- 🔄 **Real-time Sync** - Instant updates across devices
- 👥 **Group Management** - Track expenses with multiple groups
- 💰 **Smart Settlements** - Minimal transaction calculations
- 📊 **Expense Analytics** - Visual insights into spending
- 🎯 **Category Tracking** - Organize by type
- 📷 **Receipt Scanning** - OCR for quick entry (Coming soon)

### 🚀 Quick Start

```bash
# Clone the repository
git clone https://github.com/your-username/hati-v2.git

# Open in Android Studio
# Build and run

# Or use command line
./gradlew assembleDebug
```

### 🏗️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Backend**: Supabase
- **Async**: Kotlin Coroutines & Flow

### 📸 Screenshots

```
[Login Screen] → [Home Screen] → [Add Transaction] → [Settlements]
```

### 🛠️ Setup

1. **Prerequisites**
   - Android Studio Hedgehog or later
   - JDK 17
   - Android SDK 26+

2. **Supabase Configuration**
   
   Create `gradle.properties` in project root:
   ```properties
   SUPABASE_URL=your_supabase_url
   SUPABASE_ANON_KEY=your_anon_key
   ```

3. **Build**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run Tests**
   ```bash
   ./gradlew test
   ```

### 📝 Code Structure

```
app/src/main/
├── java/com/hati/v2/
│   ├── data/
│   │   ├── local/          # Room database
│   │   ├── remote/         # Supabase client
│   │   └── repository/     # Data sources
│   ├── domain/
│   │   ├── model/          # Business models
│   │   └── usecase/        # Business logic
│   ├── presentation/
│   │   ├── screen/         # UI screens
│   │   ├── components/     # Reusable components
│   │   ├── theme/          # Design system
│   │   └── animation/      # Custom animations
│   └── di/                 # Dependency injection
```

### 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

### 📜 License

This project is licensed under the MIT License - see LICENSE file for details.

### 👥 Team

- **Lead Developer**: [Your Name]
- **UI/UX**: [Designer Name]
- **Backend**: Supabase

### 🙏 Acknowledgments

- Inspired by Splitwise, Settle Up, and manga art
- Built with love using Jetpack Compose
- Community feedback and contributions

---

# ARCHITECTURE

## System Architecture

### High-Level Overview

```
┌─────────────────┐
│  Presentation   │  Jetpack Compose UI
│   (ViewModels)  │  + State Management
└────────┬────────┘
         │
┌────────▼────────┐
│     Domain      │  Business Logic
│   (UseCases)    │  + Models
└────────┬────────┘
         │
┌────────▼────────┐
│      Data       │  Repository Pattern
│ (Repositories)  │  + Data Sources
└────────┬────────┘
         │
    ┌────┴────┐
┌───▼───┐ ┌──▼────┐
│ Local │ │Remote │
│ Room  │ │Supabase│
└───────┘ └────────┘
```

### Layer Responsibilities

#### 1. Presentation Layer
- **ViewModels**: State management and UI logic
- **Screens**: Composable UI components
- **Theme**: Design system (colors, typography, components)

**Example**:
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun loadTransactions(groupId: String) {
        viewModelScope.launch {
            getTransactionsUseCase(groupId)
                .collect { result ->
                    _uiState.value = when {
                        result.isSuccess -> UiState.Success(result.getOrNull()!!)
                        else -> UiState.Error(result.exceptionOrNull()?.message)
                    }
                }
        }
    }
}
```

#### 2. Domain Layer
- **Models**: Business entities (Transaction, Group, User)
- **UseCases**: Single-responsibility business operations
- **Interfaces**: Repository contracts

**Example**:
```kotlin
class GetTransactionsByGroupUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(groupId: String): Flow<Result<List<Transaction>>> {
        return repository.getTransactionsByGroup(groupId)
    }
}
```

#### 3. Data Layer
- **Repositories**: Coordinate data from multiple sources
- **Local**: Room database for offline storage
- **Remote**: Supabase for cloud sync

**Data Flow**:
```
User Action → ViewModel → UseCase → Repository
                                    ↓
                        ┌───────────┴──────────┐
                        ↓                       ↓
                   Local (Room)          Remote (Supabase)
                        ↓                       ↓
                    Cache First           Cloud Backup
```

### Offline-First Strategy

1. **Write**: Save locally immediately, sync when online
2. **Read**: Always from local database
3. **Sync**: Background sync when connectivity restored
4. **Conflict**: Last-Write-Wins (LWW) resolution

### Dependency Injection

Using Hilt for compile-time DI:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(context: Context): HatiDatabase {
        return Room.databaseBuilder(
            context,
            HatiDatabase::class.java,
            "hati_database"
        ).build()
    }
    
    @Provides
    fun provideTransactionDao(db: HatiDatabase) = db.transactionDao()
}
```

### State Management

Using Kotlin Flow for reactive state:

```kotlin
// ViewModel
val uiState: StateFlow<UiState> = ...

// UI
val state by viewModel.uiState.collectAsState()

when (state) {
    is UiState.Loading -> LoadingIndicator()
    is UiState.Success -> ContentView(state.data)
    is UiState.Error -> ErrorView(state.message)
}
```

---

# API DOCS

## Data Models

### Transaction

```kotlin
/**
 * Represents a single expense transaction
 * 
 * @property id Unique identifier (UUID)
 * @property groupId Group this transaction belongs to
 * @property description What was purchased
 * @property amount Cost in local currency
 * @property paidBy User ID who paid
 * @property category Expense category (food, transport, etc.)
 * @property createdAt When transaction was created
 * @property updatedAt Last modification time
 */
data class Transaction(
    val id: String,
    val groupId: String,
    val description: String,
    val amount: Double,
    val paidBy: String,
    val category: String = "other",
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Group

```kotlin
/**
 * Represents a group of users sharing expenses
 * 
 * @property id Unique identifier
 * @property name Display name
 * @property members List of group members
 * @property currency ISO currency code (PHP, USD, etc.)
 */
data class Group(
    val id: String,
    val name: String,
    val description: String,
    val createdBy: String,
    val members: List<GroupMember>,
    val currency: String = "PHP",
    val createdAt: Instant,
    val updatedAt: Instant
)
```

## Repository Interface

### TransactionRepository

```kotlin
interface TransactionRepository {
    
    /**
     * Get all transactions for a group as a reactive stream
     * 
     * @param groupId Group to fetch transactions for
     * @return Flow emitting lists of transactions
     */
    fun getTransactionsByGroup(groupId: String): Flow<Result<List<Transaction>>>
    
    /**
     * Add a new transaction
     * Saves locally and syncs when online
     * 
     * @param transaction Transaction to add
     * @return Result indicating success/failure
     */
    suspend fun addTransaction(transaction: Transaction): Result<Transaction>
    
    /**
     * Update existing transaction
     * 
     * @param transaction Updated transaction data
     * @return Result indicating success/failure
     */
    suspend fun updateTransaction(transaction: Transaction): Result<Transaction>
    
    /**
     * Soft delete a transaction
     * 
     * @param transactionId ID of transaction to delete
     * @return Result indicating success/failure
     */
    suspend fun deleteTransaction(transactionId: String): Result<Unit>
    
    /**
     * Sync local changes to remote server
     * Called automatically when network available
     * 
     * @return Result with number of transactions synced
     */
    suspend fun syncLocalChanges(): Result<Int>
}
```

## Database Schema

### Tables

**transactions**
```sql
CREATE TABLE transactions (
    id TEXT PRIMARY KEY NOT NULL,
    groupId TEXT NOT NULL,
    description TEXT NOT NULL,
    amount REAL NOT NULL,
    paidBy TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'other',
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    isSynced INTEGER NOT NULL DEFAULT 0,
    isDeleted INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (groupId) REFERENCES groups(id)
);

CREATE INDEX idx_transactions_groupId ON transactions(groupId);
CREATE INDEX idx_transactions_createdAt ON transactions(createdAt);
```

**groups**
```sql
CREATE TABLE groups (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    createdBy TEXT NOT NULL,
    memberIds TEXT NOT NULL, -- Comma-separated list
    currency TEXT NOT NULL DEFAULT 'PHP',
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    isDeleted INTEGER NOT NULL DEFAULT 0
);
```

**users**
```sql
CREATE TABLE users (
    id TEXT PRIMARY KEY NOT NULL,
    email TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    avatarUrl TEXT,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);
```

## Supabase Schema

Same as Room schema but with additional RLS policies:

```sql
-- Enable Row Level Security
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;

-- Users can only see transactions from their groups
CREATE POLICY "Users can view own group transactions"
ON transactions FOR SELECT
USING (
    groupId IN (
        SELECT id FROM groups
        WHERE createdBy = auth.uid()
        OR memberIds LIKE '%' || auth.uid() || '%'
    )
);
```

---

# SETUP

## Development Environment

### Required Software

1. **Android Studio** Hedgehog (2023.1.1) or later
2. **JDK** 17
3. **Android SDK**:
   - compileSdk: 34
   - minSdk: 26
   - targetSdk: 34

### Project Setup

#### 1. Clone Repository

```bash
git clone https://github.com/your-username/hati-v2.git
cd hati-v2
```

#### 2. Configure Supabase

Create account at [supabase.com](https://supabase.com)

Run database migrations:

```sql
-- Create tables
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    groupId UUID NOT NULL,
    description TEXT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    paidBy UUID NOT NULL,
    category TEXT NOT NULL DEFAULT 'other',
    createdAt TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updatedAt TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Enable realtime
ALTER PUBLICATION supabase_realtime ADD TABLE transactions;
```

Create `gradle.properties`:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key-here
```

#### 3. Build Project

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### Running Tests

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# All tests
./gradlew test

# With coverage
./gradlew jacocoTestReport
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### Code Style

Project uses standard Kotlin conventions:

```bash
# Format code
./gradlew ktlintFormat

# Check style
./gradlew ktlintCheck
```

### Debugging

Enable logging in `BuildConfig`:

```kotlin
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}
```

View logs:
```bash
adb logcat -s HATI:V
```

---

# CONTRIBUTING

## How to Contribute

We welcome contributions! Here's how to get started:

### 1. Pick an Issue

- Check [Issues](https://github.com/your-username/hati-v2/issues)
- Look for `good-first-issue` or `help-wanted` labels
- Comment on the issue to claim it

### 2. Fork & Branch

```bash
# Fork repo on GitHub
git clone https://github.com/YOUR-USERNAME/hati-v2.git
cd hati-v2

# Create feature branch
git checkout -b feature/amazing-feature
```

### 3. Make Changes

- Follow existing code style
- Write tests for new features
- Update documentation
- Commit with clear messages:

```bash
git commit -m "feat: add receipt scanning feature

- Implement ML Kit integration
- Add camera permission handling
- Create OCR parsing logic
- Add unit tests

Closes #123"
```

### 4. Test Everything

```bash
./gradlew test
./gradlew ktlintCheck
```

### 5. Push & PR

```bash
git push origin feature/amazing-feature
```

Open Pull Request on GitHub with:
- Clear title
- Description of changes
- Link to related issue
- Screenshots (for UI changes)

### Code Review Process

1. Automated checks must pass
2. At least one approving review
3. No merge conflicts
4. Documentation updated

### Commit Message Format

```
<type>: <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code restructure
- `test`: Tests
- `chore`: Build/tooling

### Questions?

- Open a [Discussion](https://github.com/your-username/hati-v2/discussions)
- Join our [Discord](https://discord.gg/hati-v2)
- Email: support@hati-app.com

---

## Additional Resources

- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)
- [Supabase Docs](https://supabase.com/docs)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)

---

**Last Updated**: February 2026
**Version**: 2.0.0
