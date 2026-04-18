# Mira

An Android personal finance app for tracking income and expenses. Supports manual entry, OCR scanning, and planned payment gateway integrations (Momo, ZaloPay, bank sync).

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 11) |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt (Dagger 2) |
| Database | Room 3 |
| Async | Kotlin Coroutines + Flow |
| Network | OkHttp 3 |
| Security | AndroidX Biometric, EncryptedSharedPreferences |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

## Architecture

Three-layer Clean Architecture with strict dependency rules:

```
Presentation (Compose + ViewModel)
        ↓
   Domain (Use Cases, Models, Repository Interfaces)
        ↓
    Data (Room, Network, Repository Implementations)
```

### Key Patterns

- **UI State**: `StateFlow<UiState<T>>` with `stateIn(WhileSubscribed(5000))`
- **One-time events**: `SharedFlow(replay = 0)` collected in `LaunchedEffect`
- **Error handling**: `Result<T>` from repositories, `.catch {}` in use cases
- **Soft delete**: `isDeleted` flag on all records — no hard deletes
- **Use cases**: `operator fun invoke()` for DSL-style calling

## Security

- **Biometric lock**: Fingerprint/face authentication gate before app access (`BiometricLockScreen`)
- **Secure storage**: Auth tokens stored in `EncryptedSharedPreferences` via `SecurePreferences`
- **Network security**: SSL certificate pinning for `api.mira.vn` (production), auth token injection via `AuthInterceptor`, security headers on every request (`X-App-Version`, `X-Platform`)
- **Debug safety**: Certificate pinning and logging redaction of `Authorization`/`Cookie` headers in release builds only

## Project Structure

```
app/src/main/java/com/apollo/mira/
├── MiraApp.kt                          # @HiltAndroidApp
├── MainActivity.kt                     # @AndroidEntryPoint
├── di/
│   ├── DatabaseModule.kt
│   ├── DispatcherModule.kt             # @Named IO/Main/Default dispatchers
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
├── domain/
│   ├── model/Transaction.kt            # Immutable domain models
│   ├── repository/TransactionRepository.kt
│   └── usecase/TransactionUseCases.kt
├── data/
│   ├── local/
│   │   ├── MiraDatabase.kt
│   │   ├── dao/TransactionDao.kt
│   │   └── entity/TransactionEntity.kt
│   ├── mapper/TransactionMapper.kt
│   ├── network/NetworkSecurity.kt      # OkHttp client, cert pinning, interceptors
│   └── repository/TransactionRepositoryImpl.kt
├── presentation/
│   ├── common/UiState.kt               # Sealed: Loading, Empty, Success, Error
│   ├── dashboard/
│   └── add_transaction/
└── security/
    ├── MiraBiometricManager.kt         # BiometricPrompt wrapper
    ├── BiometricLockScreen.kt          # Lock screen composable + ViewModel
    └── SecurePreferences.kt            # EncryptedSharedPreferences wrapper
```

## Building & Running

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
adb shell am start -n com.apollo.mira/.MainActivity

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean build
```

## Known Gaps / TODO

- **Navigation**: Callback-based; needs `navigation-compose` for type-safe routing
- **Add Transaction Screen**: ViewModel and form state exist; Composable not wired to navigation
- **Transaction Detail Screen**: Not implemented
- **Soft delete UI**: `softDelete` DAO method not connected to any UI action
- **OCR Integration**: `DataSource.OCR` enum exists, no implementation
- **Payment Gateways**: `DataSource.MOMO`, `ZALOPAY`, `BANK_SYNC` planned, not implemented
- **Certificate pinning**: Placeholder hashes in `NetworkSecurity` — replace before production
- **Biometric gate**: `BiometricLockScreen` built but not yet wired into `MainActivity`
