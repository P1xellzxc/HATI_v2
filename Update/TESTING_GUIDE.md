# HATI v2 - Complete Testing Guide

## Quick Start

```bash
# Run all tests
./gradlew test

# Run unit tests only  
./gradlew testDebugUnitTest

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate coverage report
./gradlew jacocoTestReport
```

## Test Structure

```
app/src/
├── test/                      # Unit tests
│   ├── viewmodel/
│   ├── usecase/
│   ├── repository/
│   └── util/
└── androidTest/               # Integration & UI tests
    ├── screen/
    ├── dao/
    └── flow/
```

## Key Test Files

### 1. ViewModel Test Template
```kotlin
@OptIn(ExperimentalCoroutinesTest::class)
class HomeViewModelTest {
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(/* mock dependencies */)
    }
    
    @Test
    fun `test name in backticks`() = runTest {
        // Given - Setup
        // When - Action  
        // Then - Assertion
    }
}
```

### 2. Repository Test Template
```kotlin
@RunWith(AndroidJUnit4::class)
class TransactionRepositoryTest {
    private lateinit var database: HatiDatabase
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            context, HatiDatabase::class.java
        ).build()
    }
}
```

### 3. UI Test Template
```kotlin
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun test() {
        composeTestRule.setContent { LoginScreen() }
        composeTestRule.onNodeWithText("Login").assertExists()
    }
}
```

## Testing Priorities

1. ✅ Critical user flows (login, add transaction)
2. ✅ Data persistence and sync
3. ✅ Error handling
4. ✅ Edge cases
5. ⚠️ Performance
6. ⚠️ Accessibility

## Required Dependencies

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("app.cash.turbine:turbine:1.0.0")

androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```

## Coverage Goals

- Unit Tests: 80%+ coverage
- Integration Tests: Critical paths
- UI Tests: Main user flows

## CI/CD Integration

See `.github/workflows/android-ci.yml` for automated testing setup.

