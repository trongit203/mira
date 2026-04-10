# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mira is an Android financial transaction management application built with **Clean Architecture**, **MVVM**, **Jetpack Compose**, and **Hilt dependency injection**. The app supports manual entry, OCR, and payment gateway integrations (Momo, ZaloPay, bank sync) for tracking income and expenses.

## Architecture

### Three-Layer Clean Architecture

The codebase follows Clean Architecture with strict layer separation:

1. **Domain Layer** (`domain/`)
   - Pure business logic, no Android dependencies
   - Contains:
     - **Models** (`domain/model/`): Immutable data classes (`Transaction`, `DashboardSummary`)
     - **Repository Interfaces** (`domain/repository/`): Contracts for data operations
     - **Use Cases** (`domain/usecase/`): Business logic orchestration
   - All domain models are immutable `data class` types
   - Returns `Flow<T>` for reactive streams and `Result<T>` for error handling

2. **Data Layer** (`data/`)
   - Implements domain repository contracts
   - Contains:
     - **Entities** (`data/local/entity/`): Mutable Room database models
     - **DAOs** (`data/local/dao/`): Room query interfaces (returns `Flow<T>`)
     - **Mappers** (`data/mapper/`): Bidirectional Entity ↔ Domain conversion
     - **Repository Implementations** (`data/repository/`): Bridges domain and persistence
   - Uses **soft delete pattern** (`isDeleted` flag) for data preservation
   - All queries filter `WHERE isDeleted = 0`

3. **Presentation Layer** (`presentation/`)
   - Jetpack Compose UI with MVVM pattern
   - Contains:
     - **ViewModels**: `@HiltViewModel` with `StateFlow` for UI state
     - **Screens**: `@Composable` functions with lifecycle-aware collection
     - **UI State**: Sealed classes (`UiState<T>`, `DashboardEvent`)
   - Uses `collectAsStateWithLifecycle()` for safe Flow collection
   - `SharedFlow` with `replay = 0` for one-time events (navigation, snackbars)

### Dependency Flow

```
UI Layer → ViewModel → Use Cases → Repository Interface (domain)
                                         ↓
                              Repository Implementation (data)
                                         ↓
                                   DAO → Entity → Room Database
```

### State Management Patterns

- **UI State**: `StateFlow<UiState<T>>` with `stateIn(SharingStarted.WhileSubscribed(5000))`
  - Auto-stops collection 5 seconds after backgrounding
  - Survives configuration changes via `viewModelScope`

- **One-Time Events**: `SharedFlow<Event>` with `replay = 0`
  - Prevents stale event notifications
  - Collected in `LaunchedEffect` for side effects

- **Reactive Data**: `Flow<T>` from Room DAOs
  - Cold flows, lazy evaluation
  - Automatically update UI on database changes

### Dependency Injection (Hilt)

- **Application**: `@HiltAndroidApp` on `MiraApplication`
- **Activities**: `@AndroidEntryPoint` on `MainActivity`
- **ViewModels**: `@HiltViewModel` with `@Inject constructor()`
- **Use Cases/Repositories**: Constructor injection with `@Inject`
- **Named Qualifiers**: `@Named("IO")` for `CoroutineDispatcher` (not yet implemented in modules)

**Note**: Hilt modules for Room database and dispatcher providers are not yet created. Future work should add:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao
}

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @Named("IO")
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO
}
```

## Common Development Commands

### Building

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build and install on connected device
./gradlew installDebug

# Clean build artifacts
./gradlew clean

# Full clean build
./gradlew clean build
```

### Testing

```bash
# Run unit tests (JUnit)
./gradlew test
# Or specific variant:
./gradlew testDebugUnitTest

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Run all tests
./gradlew test connectedAndroidTest
```

### Code Quality

```bash
# Compile Kotlin sources
./gradlew compileDebugKotlin

# Check dependencies
./gradlew dependencies
```

### Running the App

```bash
# Install and launch on connected device
./gradlew installDebug
adb shell am start -n com.apollo.mira/.MainActivity
```

## Key Architectural Conventions

### 1. Immutable Domain, Mutable Entities

Domain models use `val` (immutable) for type safety:
```kotlin
// Domain
data class Transaction(val id: Long, val amount: Double, ...)
```

Data entities use `var` (mutable) for Room requirements:
```kotlin
// Data
data class TransactionEntity(var id: Long = 0, var amount: Double = 0.0, ...)
```

Always use `TransactionMapper` to convert between layers.

### 2. Result<T> Over Exceptions

Repository methods return `Result<T>` for type-safe error handling:
```kotlin
suspend fun addTransaction(transaction: Transaction): Result<Transaction>
```

Call sites handle errors explicitly:
```kotlin
repository.addTransaction(tx)
    .onSuccess { /* handle success */ }
    .onFailure { /* handle error */ }
```

### 3. Flow Error Handling with .catch()

Use `.catch {}` in use cases to transform upstream errors into `UiState.Error`:
```kotlin
repository.getTransactions()
    .map { /* transform */ }
    .catch { emit(UiState.Error(it.message, retryable = true)) }
```

**Important**: `.catch {}` only catches upstream errors (repository/DAO), not collector errors.

### 4. ViewModel Event Pattern

For one-time events (navigation, snackbars), use `SharedFlow`:
```kotlin
private val _events = MutableSharedFlow<DashboardEvent>(replay = 0)
val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()

// Emit in action handlers
viewModelScope.launch {
    _events.emit(DashboardEvent.ShowSnackbar("Success"))
}
```

Collect in Composables with `LaunchedEffect`:
```kotlin
LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
        when (event) {
            is DashboardEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            // ...
        }
    }
}
```

### 5. Soft Delete Pattern

Never hard-delete records. Use soft delete for sync and recovery:
```kotlin
@Query("UPDATE transactions SET isDeleted = 1 WHERE id = :id")
suspend fun softDelete(id: Long)
```

All read queries must filter: `WHERE isDeleted = 0`

### 6. Use Case Invoke Pattern

Use cases use `operator fun invoke()` for DSL-like calling:
```kotlin
class GetDashboardSummaryUseCase @Inject constructor(...) {
    operator fun invoke(): Flow<UiState<DashboardSummary>> = ...
}

// Usage in ViewModel
val uiState = getDashboardSummary() // calls invoke()
    .stateIn(...)
```

### 7. Sealed Classes for Exhaustiveness

Use sealed classes for states and events to get compiler-enforced exhaustive `when` checks:
```kotlin
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val retryable: Boolean) : UiState<Nothing>()
}
```

## Technology Stack

- **Language**: Kotlin (JVM target 11)
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **UI**: Jetpack Compose (Material 3)
- **DI**: Hilt (Dagger 2)
- **Database**: Room 3 (alpha)
- **Async**: Kotlin Coroutines + Flow
- **Architecture**: Clean Architecture + MVVM

## Navigation (Future Implementation)

Current navigation uses callback functions passed to Composables:
```kotlin
DashboardScreen(
    onNavigateToAddTransaction = { /* TODO */ },
    onNavigateToDetail = { id -> /* TODO */ }
)
```

Future work should implement `androidx.navigation:navigation-compose` for type-safe routing and back stack management.

## Known Gaps / TODO

1. **Hilt Modules**: Database and dispatcher provider modules not yet created
2. **Navigation**: Currently callback-based, needs Navigation Compose integration
3. **AppDatabase**: Room database class not yet defined
4. **Delete Implementation**: `softDelete` logic not wired to UI
5. **Add Transaction Screen**: UI not yet implemented (TODOs in MainActivity)
6. **Transaction Detail Screen**: Not yet implemented
7. **OCR Integration**: `DataSource.OCR` enum exists but no implementation
8. **Payment Gateway Sync**: `DataSource.MOMO`, `ZALOPAY`, `BANK_SYNC` planned but not implemented

## File Organization

```
app/src/main/java/com/apollo/mira/
├── MiraApplication.kt              # Hilt entry point
├── MainActivity.kt                 # Activity with Compose setContent
├── domain/
│   ├── model/
│   │   └── Transaction.kt          # Immutable domain models
│   ├── repository/
│   │   └── TransactionRepository.kt # Repository contract
│   └── usecase/
│       └── TransactionUseCases.kt  # Business logic
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   └── TransactionEntity.kt # Mutable Room entity
│   │   └── dao/
│   │       └── TransactionDao.kt    # Room queries
│   ├── mapper/
│   │   └── TransactionMapper.kt     # Entity ↔ Domain conversion
│   └── repository/
│       └── TransactionRepositoryImpl.kt # Repository implementation
└── presentation/
    ├── common/
    │   └── UiState.kt               # Sealed UI state classes
    └── dashboard/
        ├── DashboardScreen.kt       # Composable UI
        ├── DashboardViewModel.kt    # State management
        └── DashboardUiState.kt      # UI events
```

## Code Comments

Note: Codebase contains mixed English/Vietnamese comments. This appears to be a learning/training project.
