# Giải thích kiến trúc Mira: MVVM + Clean Architecture + Hilt/Dagger

> Tài liệu này giải thích **tại sao** code được viết như vậy, không chỉ **cái gì**.
> Mọi ví dụ đều lấy trực tiếp từ source code của project Mira.

---

## Mục lục

1. [MVVM là gì và tại sao dùng nó](#1-mvvm-là-gì-và-tại-sao-dùng-nó)
2. [Clean Architecture — 3 tầng trong Mira](#2-clean-architecture--3-tầng-trong-mira)
3. [Dependency Injection là gì](#3-dependency-injection-là-gì)
4. [Dagger — nền tảng của Hilt](#4-dagger--nền-tảng-của-hilt)
5. [Hilt — Dagger đơn giản hóa cho Android](#5-hilt--dagger-đơn-giản-hóa-cho-android)
6. [Giải thích toàn bộ annotation](#6-giải-thích-toàn-bộ-annotation)
7. [Luồng dữ liệu hoàn chỉnh trong Mira](#7-luồng-dữ-liệu-hoàn-chỉnh-trong-mira)
8. [StateFlow vs SharedFlow — khi nào dùng cái nào](#8-stateflow-vs-sharedflow--khi-nào-dùng-cái-nào)
9. [Câu hỏi phỏng vấn thường gặp](#9-câu-hỏi-phỏng-vấn-thường-gặp)

---

## 1. MVVM là gì và tại sao dùng nó

### Định nghĩa

MVVM = **Model – View – ViewModel**

| Thành phần | Vai trò | Trong Mira |
|---|---|---|
| **Model** | Dữ liệu và logic nghiệp vụ | `Transaction`, `DashboardSummary`, các UseCase |
| **View** | Hiển thị UI, nhận input người dùng | `DashboardScreen.kt`, `BiometricLockScreen.kt` |
| **ViewModel** | Cầu nối, giữ UI state, xử lý action | `DashboardViewModel.kt`, `BiometricViewModel.kt` |

### Vấn đề MVVM giải quyết

**Trước MVVM (Activity/Fragment làm tất cả):**
```
Activity {
    fun onCreate() {
        val data = database.query()   // ❌ trực tiếp gọi DB trong UI
        textView.text = data.name     // ❌ business logic lẫn với UI
        // Khi xoay màn hình → Activity destroy → mất data
    }
}
```

**Với MVVM:**
```kotlin
// ViewModel sống độc lập với Activity lifecycle
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase
) : ViewModel() {

    val uiState = getDashboardSummary()   // ViewModel chỉ biết UseCase, không biết UI
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )
}
```

```kotlin
// Screen chỉ observe và render — không có logic
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Render based on uiState
}
```

**Lợi ích:**
- Xoay màn hình → ViewModel vẫn sống, data không mất
- Unit test ViewModel mà không cần Android emulator
- UI chỉ render, không xử lý business logic

---

## 2. Clean Architecture — 3 tầng trong Mira

```
┌─────────────────────────────────────────┐
│         PRESENTATION LAYER              │
│   DashboardScreen ← DashboardViewModel  │
│   BiometricLockScreen ← BiometricVM     │
│   SecuritySettingsScreen ← SecurityVM   │
└──────────────┬──────────────────────────┘
               │ chỉ gọi UseCase
               ▼
┌─────────────────────────────────────────┐
│            DOMAIN LAYER                 │
│   GetDashboardSummaryUseCase            │
│   AddTransactionUseCase                 │
│   TransactionRepository (interface)     │
│   Transaction, DashboardSummary         │
└──────────────┬──────────────────────────┘
               │ chỉ gọi Repository interface
               ▼
┌─────────────────────────────────────────┐
│             DATA LAYER                  │
│   TransactionRepositoryImpl             │
│   TransactionDao (Room)                 │
│   TransactionEntity                     │
│   TransactionMapper                     │
└─────────────────────────────────────────┘
```

### Tại sao phải tách tầng?

**Quy tắc vàng:** Dependency chỉ trỏ vào trong (Domain không biết Data tồn tại).

```kotlin
// ✅ ĐÚNG — Domain chỉ biết interface
class GetDashboardSummaryUseCase @Inject constructor(
    private val repository: TransactionRepository  // interface, không phải Impl
)

// ❌ SAI — Domain biết chi tiết Data layer
class GetDashboardSummaryUseCase @Inject constructor(
    private val repository: TransactionRepositoryImpl  // cụ thể implementation
)
```

Khi sau này đổi từ Room sang SQLDelight, chỉ cần viết `SQLDelightRepositoryImpl` mới và đổi binding trong `RepositoryModule`. Domain và Presentation không cần sửa gì.

---

## 3. Dependency Injection là gì

### Bài toán không có DI

```kotlin
class DashboardViewModel : ViewModel() {
    // ❌ ViewModel tự tạo dependencies — tightly coupled
    private val dao = TransactionDao(database)
    private val mapper = TransactionMapper()
    private val repository = TransactionRepositoryImpl(dao, mapper, Dispatchers.IO)
    private val useCase = GetDashboardSummaryUseCase(repository)
}
```

**Vấn đề:**
- Muốn test → phải tạo real database
- Muốn đổi implementation → phải sửa trong từng class
- Khởi tạo sai thứ tự → crash

### DI giải quyết bằng cách "đảo ngược điều khiển"

```kotlin
// ✅ ViewModel nhận dependencies từ bên ngoài — loosely coupled
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    private val addTransaction: AddTransactionUseCase
) : ViewModel()
```

Hilt sẽ tự lo việc tạo `GetDashboardSummaryUseCase`, và để tạo nó cần `TransactionRepository`, và để có Repository cần `TransactionDao`, cần `MiraDatabase`, cần `Context`... Hilt tự giải quyết toàn bộ chuỗi này.

---

## 4. Dagger — nền tảng của Hilt

Hilt được xây dựng trên Dagger 2. Hiểu Dagger giúp bạn debug khi Hilt báo lỗi.

### Dagger hoạt động thế nào

Dagger là **compile-time** DI framework. Tại thời điểm build (không phải runtime), Dagger:
1. Đọc tất cả annotation (`@Module`, `@Provides`, `@Inject`...)
2. Xây dựng **dependency graph** — biết class nào cần class nào
3. **Sinh code Java** — tạo các `Factory` và `Component` class
4. Khi chạy app, code sinh ra được gọi để tạo object

```
Source code của bạn         Dagger sinh ra (bạn không thấy)
─────────────────           ──────────────────────────────
DashboardViewModel    →     DashboardViewModel_HiltModules.java
DatabaseModule        →     DaggerAppComponent.java
@Inject constructor   →     DashboardViewModel_Factory.java
```

**Lợi ích compile-time:** Nếu bạn quên provide một dependency, **build sẽ fail** thay vì app crash lúc runtime.

---

## 5. Hilt — Dagger đơn giản hóa cho Android

Hilt là lớp bọc trên Dagger, tự động:
- Tạo các Dagger Component cho từng Android component (Application, Activity, ViewModel...)
- Quản lý lifecycle của component
- Xử lý boilerplate code

### Bước 1: Khai báo entry point — `@HiltAndroidApp`

```kotlin
// MiraApp.kt
@HiltAndroidApp
class MiraApp : Application()
```

`@HiltAndroidApp` bảo Hilt: *"App bắt đầu từ đây. Sinh code để tạo ApplicationComponent."*

Đây là **bắt buộc** — không có annotation này, toàn bộ Hilt không hoạt động.

### Bước 2: Kích hoạt injection trong Android class — `@AndroidEntryPoint`

```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity()
```

`@AndroidEntryPoint` bảo Hilt: *"Class này muốn nhận dependency injection. Sinh code để inject vào đây."*

Cần đặt trên mọi Activity/Fragment muốn dùng Hilt.

---

## 6. Giải thích toàn bộ annotation

### `@Module`

**Ý nghĩa:** Đánh dấu class là nơi khai báo cách tạo dependencies.

```kotlin
@Module  // ← "Đây là nơi tôi dạy Hilt cách tạo object"
@InstallIn(SingletonComponent::class)
object DatabaseModule { ... }
```

Có 2 dạng Module:
- `object` — dùng khi có `@Provides` (static functions)
- `abstract class` — dùng khi có `@Binds` (abstract functions)

```kotlin
// DatabaseModule dùng object vì có @Provides
object DatabaseModule { ... }

// RepositoryModule dùng abstract class vì có @Binds
abstract class RepositoryModule { ... }
```

---

### `@InstallIn`

**Ý nghĩa:** Khai báo Module này thuộc về Component nào — quyết định **scope sống** của dependency.

```kotlin
@Module
@InstallIn(SingletonComponent::class)  // ← sống suốt vòng đời của Application
object DatabaseModule { ... }
```

**Các Component phổ biến:**

| Component | Tương đương Android | Scope | Dùng khi |
|---|---|---|---|
| `SingletonComponent` | `Application` | Suốt đời app | Database, Repository, Retrofit |
| `ActivityComponent` | `Activity` | Sống cùng Activity | Thứ cần Activity context |
| `ViewModelComponent` | `ViewModel` | Sống cùng ViewModel | Thứ chỉ cần trong ViewModel |
| `FragmentComponent` | `Fragment` | Sống cùng Fragment | Hiếm dùng với Compose |

**Trong Mira:** Tất cả Module đều dùng `SingletonComponent` vì `MiraDatabase`, `Repository`, `Dispatcher` đều nên là singleton — tạo 1 lần, dùng suốt.

---

### `@Provides`

**Ý nghĩa:** Đánh dấu function "đây là cách tạo object X". Dagger gọi function này khi cần X.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides          // ← "Khi ai cần MiraDatabase, gọi function này"
    @Singleton
    fun provideMiraDatabase(@ApplicationContext context: Context): MiraDatabase =
        Room.databaseBuilder(context, MiraDatabase::class.java, MiraDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides          // ← "Khi ai cần TransactionDao, gọi function này"
    fun provideTransactionDao(database: MiraDatabase): TransactionDao =
        database.transactionDao()
    // Dagger tự biết cần MiraDatabase → gọi provideMiraDatabase() trước
}
```

**Quy tắc:** Return type của `@Provides` function = type mà Dagger sẽ provide.

**Dùng khi:** Cần tạo object phức tạp, hoặc class đến từ thư viện third-party (không thể thêm `@Inject` vào constructor của nó).

---

### `@Singleton`

**Ý nghĩa:** Chỉ tạo 1 instance duy nhất trong suốt lifecycle của Component.

```kotlin
@Provides
@Singleton  // ← Chỉ tạo 1 MiraDatabase cho toàn app
fun provideMiraDatabase(...): MiraDatabase = ...
```

**Không có `@Singleton`:** Mỗi lần ai đó inject `MiraDatabase`, Dagger tạo instance mới — gây ra nhiều DB connection, inconsistent state.

**Với `@Singleton`:** Lần đầu inject → tạo instance. Mọi lần sau → trả về cùng instance đó.

```kotlin
// TransactionDao KHÔNG cần @Singleton
@Provides
fun provideTransactionDao(database: MiraDatabase): TransactionDao =
    database.transactionDao()
// DAO là interface proxy nhẹ, tạo mới cũng không tốn kém
// Nhưng database được inject vào DAO đã là @Singleton → vẫn chỉ có 1 DB
```

---

### `@Binds`

**Ý nghĩa:** Nói với Dagger "khi ai cần interface X, hãy inject implementation Y vào".

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl    // ← implementation
    ): TransactionRepository               // ← interface (return type)
}
```

**So sánh `@Binds` vs `@Provides`:**

```kotlin
// Dùng @Provides — verbose, tốn thêm 1 bước khởi tạo
@Provides
fun provideRepository(impl: TransactionRepositoryImpl): TransactionRepository = impl

// Dùng @Binds — compile-time only, không tạo thêm object
@Binds
abstract fun bindRepository(impl: TransactionRepositoryImpl): TransactionRepository
```

`@Binds` hiệu quả hơn vì:
1. Là `abstract` → không sinh code tạo object tại runtime
2. Dagger biết tại compile-time rằng đây chỉ là type alias
3. Bắt buộc phải dùng `abstract class` (không thể dùng `object`)

**Điều kiện để `@Binds` hoạt động:** Implementation (`TransactionRepositoryImpl`) phải có `@Inject constructor` để Dagger biết cách tạo nó.

```kotlin
class TransactionRepositoryImpl @Inject constructor(  // ← bắt buộc
    private val dao: TransactionDao,
    private val mapper: TransactionMapper,
    @Named("IO") private val dispatcher: CoroutineDispatcher
): TransactionRepository
```

---

### `@Named`

**Ý nghĩa:** Phân biệt nhiều dependency cùng type.

**Vấn đề:** Mira cần 3 `CoroutineDispatcher` khác nhau. Nếu chỉ dùng type, Dagger không biết inject cái nào.

```kotlin
// ❌ Ambiguous — Dagger không biết IO hay Main hay Default
fun someRepo(dispatcher: CoroutineDispatcher) // type trùng nhau!
```

**Giải pháp với `@Named`:**

```kotlin
// DispatcherModule.kt — khai báo
@Provides @Singleton @Named("IO")
fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO

@Provides @Singleton @Named("Main")
fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

@Provides @Singleton @Named("Default")
fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
```

```kotlin
// TransactionRepositoryImpl.kt — sử dụng
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val mapper: TransactionMapper,
    @Named("IO") private val dispatcher: CoroutineDispatcher  // ← chỉ định rõ cái nào
)
```

**Thay thế cho `@Named`:** Tạo custom qualifier annotation (type-safe hơn, nhưng verbose hơn):

```kotlin
// Cách type-safe hơn (không dùng trong Mira nhưng nên biết)
@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
```

---

### `@Inject`

**Ý nghĩa:** Có 2 cách dùng khác nhau.

#### Cách 1: Constructor Injection — dạy Dagger cách tạo class

```kotlin
class GetDashboardSummaryUseCase @Inject constructor(
    private val repository: TransactionRepository  // Dagger tự inject
)
```

Khi có `@Inject constructor`:
- Không cần `@Provides` trong Module
- Dagger đọc constructor → biết dependencies cần thiết → tự inject

```kotlin
class TransactionMapper @Inject constructor()  // không cần dependency nào
```

#### Cách 2: Field Injection — inject vào field của Android class

```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject                           // ← inject vào field
    lateinit var securePreferences: SecurePreferences

    override fun onCreate(...) {
        // securePreferences đã được inject trước onCreate
        val enabled = securePreferences.isBiometricEnabled
    }
}
```

**Khi nào dùng field injection:** Các Android class mà bạn không kiểm soát constructor (Activity, Fragment, Service). Với ViewModel và UseCase thì dùng constructor injection.

---

### `@HiltViewModel`

**Ý nghĩa:** Đánh dấu ViewModel để Hilt quản lý lifecycle và injection.

```kotlin
@HiltViewModel                           // ← bắt buộc
class DashboardViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    private val addTransaction: AddTransactionUseCase
) : ViewModel()
```

**Tại sao cần cả 2 `@HiltViewModel` và `@Inject`?**

- `@HiltViewModel` → Hilt biết đây là ViewModel, tạo factory đặc biệt tôn trọng ViewModel lifecycle
- `@Inject constructor` → Dagger biết cách tạo instance và inject dependencies

Thiếu `@HiltViewModel` → Hilt không tạo factory → crash `Cannot create an instance` (lỗi bạn vừa gặp với `BiometricViewModel`).

**Sử dụng trong Compose:**

```kotlin
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()  // ← Hilt tạo và quản lý
)
```

---

### `@ApplicationContext` và `@ActivityContext`

**Ý nghĩa:** Qualifier có sẵn của Hilt để inject Context đúng loại.

```kotlin
@Provides
fun provideMiraDatabase(
    @ApplicationContext context: Context  // ← Application context, không phải Activity
): MiraDatabase = Room.databaseBuilder(context, ...)
```

**Tại sao phân biệt?**

- `Application Context`: Sống suốt đời app — an toàn để inject vào `@Singleton`
- `Activity Context`: Sống cùng Activity — **KHÔNG ĐƯỢC** inject vào `@Singleton` (gây memory leak!)

```kotlin
// ❌ Memory leak — Activity context bị giữ bởi Singleton
@Singleton
class SomeSingleton @Inject constructor(
    private val context: Context  // Dagger không biết đây là loại nào!
)

// ✅ Đúng
@Singleton
class SomeSingleton @Inject constructor(
    @ApplicationContext private val context: Context
)
```

---

### `@HiltAndroidApp`

```kotlin
@HiltAndroidApp
class MiraApp : Application()
```

Sinh ra:
- `AppComponent` — root Dagger component
- Code khởi tạo DI khi Application start
- Entry point cho toàn bộ dependency graph

---

### `@AndroidEntryPoint`

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity()
```

Sinh ra:
- `MainActivity_GeneratedInjector` interface
- Code inject fields vào Activity trước `super.onCreate()`
- Cho phép dùng `hiltViewModel()` trong Composable bên trong Activity này

---

## 7. Luồng dữ liệu hoàn chỉnh trong Mira

### Ví dụ: User mở Dashboard → data hiển thị

```
1. App start
   MiraApp (@HiltAndroidApp)
   └── Hilt khởi tạo: MiraDatabase, TransactionDao, Mapper,
       TransactionRepositoryImpl, GetDashboardSummaryUseCase

2. MainActivity.onCreate()
   @AndroidEntryPoint → inject SecurePreferences vào field
   setContent { MaterialTheme { NavHost { DashboardScreen() } } }

3. DashboardScreen Composable xuất hiện
   hiltViewModel() → Hilt tạo DashboardViewModel với dependencies đã có sẵn

4. DashboardViewModel init
   getDashboardSummary()  →  gọi GetDashboardSummaryUseCase.invoke()
   .stateIn(...)          →  bắt đầu collect, initialValue = UiState.Loading

5. GetDashboardSummaryUseCase.invoke()
   repository.getRecentTransactions(20)
   └── TransactionRepositoryImpl.getRecentTransactions()
       └── dao.getRecentTransactions(20)            ← Room query
           .map { mapper.toDomainList(it) }         ← Entity → Domain
           .flowOn(Dispatchers.IO)                  ← chạy trên IO thread

6. Room emit data → Flow chạy ngược lên
   TransactionRepositoryImpl → UseCase.map() → UiState.Success(data)
   → stateIn() cập nhật uiState

7. DashboardScreen collectAsStateWithLifecycle()
   uiState thay đổi → Compose recompose → hiện danh sách transaction
```

### Ví dụ: User thêm transaction

```
User nhấn nút "Thêm"
└── DashboardScreen gọi viewModel.onQuickAddTransaction(transaction)
    └── DashboardViewModel launch coroutine
        └── addTransaction(transaction)        ← AddTransactionUseCase.invoke()
            └── require(amount > 0)            ← validate
            └── repository.addTransaction()    ← TransactionRepositoryImpl
                └── withContext(Dispatchers.IO) ← switch to IO thread
                    └── dao.insert(entity)     ← Room insert
                        └── return id
            └── transaction.copy(id = id)      ← trả về domain object với id mới
        .onSuccess → _events.emit(ShowSnackbar("Đã thêm ✓"))
        .onFailure → _events.emit(ShowSnackbar(error.message))
```

```
DashboardScreen LaunchedEffect {
    viewModel.events.collect { event ->
        when (event) {
            is ShowSnackbar → snackbarHostState.showSnackbar(event.message)
            // Room tự động re-emit Flow → uiState cập nhật không cần làm thêm gì
        }
    }
}
```

---

## 8. StateFlow vs SharedFlow — khi nào dùng cái nào

### StateFlow — cho UI state

```kotlin
// DashboardViewModel.kt
val uiState = getDashboardSummary()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading
    )
```

| Đặc điểm | StateFlow |
|---|---|
| Luôn có giá trị | ✅ (initialValue) |
| Replay cho subscriber mới | ✅ (1 giá trị gần nhất) |
| Dành cho | Trạng thái UI (Loading/Success/Error) |
| Subscriber mới join | Nhận ngay giá trị hiện tại |

`SharingStarted.WhileSubscribed(5_000)`: Thu thập dừng lại 5 giây sau khi không còn subscriber (app background). Khi user quay lại — resume ngay, không cần cold start lại.

### SharedFlow — cho one-time events

```kotlin
// DashboardViewModel.kt
private val _events = MutableSharedFlow<DashboardEvent>(replay = 0)
val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()
```

| Đặc điểm | SharedFlow (replay=0) |
|---|---|
| Luôn có giá trị | ❌ (không có initial) |
| Replay cho subscriber mới | ❌ (replay=0) |
| Dành cho | Events một lần: navigation, snackbar, dialog |
| Subscriber mới join | KHÔNG nhận event cũ |

**Tại sao events dùng SharedFlow không dùng StateFlow?**

```kotlin
// ❌ Nếu dùng StateFlow cho event "Thêm thành công"
// User thêm transaction → hiện snackbar ✓
// User xoay màn hình → Screen recompose → collectAsState() nhận lại
//   UiState("Thêm thành công") → hiện snackbar lần nữa ❌

// ✅ SharedFlow(replay=0) → event chỉ được nhận đúng 1 lần
//    Subscriber mới (sau xoay màn hình) không nhận lại event cũ
```

---

## 9. Câu hỏi phỏng vấn thường gặp

**Q: `@Binds` khác `@Provides` thế nào?**

`@Provides` dùng function body để tạo object (runtime). `@Binds` là abstract function, chỉ ánh xạ type tại compile-time, hiệu quả hơn. Dùng `@Binds` khi bạn chỉ muốn map interface → implementation đã có `@Inject constructor`.

---

**Q: Tại sao inject `CoroutineDispatcher` thay vì dùng `Dispatchers.IO` trực tiếp?**

Trong production code `Dispatchers.IO` là bình thường. Nhưng trong unit test, bạn cần `StandardTestDispatcher` để control timing, chạy coroutine synchronously. Nếu hard-code `Dispatchers.IO` → không thể swap trong test → test bị flaky. Với `@Named("IO") dispatcher: CoroutineDispatcher` → test inject `TestDispatcher` thay vào.

---

**Q: `SingletonComponent` vs `@Singleton` khác nhau thế nào?**

- `@InstallIn(SingletonComponent::class)` → Module được install vào Application-scoped component
- `@Singleton` → Instance đó chỉ được tạo 1 lần trong component đó

Có thể có Module trong `SingletonComponent` mà không dùng `@Singleton` → Dagger tạo mới mỗi lần inject (unscoped).

---

**Q: Tại sao `RepositoryModule` là `abstract class` còn `DatabaseModule` là `object`?**

- `@Binds` yêu cầu function là `abstract` → class phải là `abstract class`
- `@Provides` yêu cầu function có body → class phải có thể instantiate hoặc là `object` (companion)
- Không thể mix `@Binds` và `@Provides` trong cùng 1 class trừ khi tách ra `companion object`

---

**Q: `hiltViewModel()` và `viewModel()` khác nhau?**

- `viewModel()` (AndroidX vanilla): dùng `ViewModelProvider.Factory` mặc định, chỉ tạo được ViewModel không có constructor parameter
- `hiltViewModel()` (Hilt): dùng Hilt-generated factory, biết cách inject dependencies vào ViewModel constructor

---

**Q: Flow error handling — `.catch{}` vs `try/catch`?**

```kotlin
repository.getTransactions()
    .map { ... }
    .catch { emit(UiState.Error(...)) }  // ✅ chỉ bắt UPSTREAM error
```

`.catch {}` chỉ bắt exception phát sinh từ upstream (dao, mapper). Exception từ collector (UI code) không bị catch ở đây — đúng behavior. `try/catch` bọc toàn bộ collector sẽ nuốt cả exception của UI, che giấu bug.
