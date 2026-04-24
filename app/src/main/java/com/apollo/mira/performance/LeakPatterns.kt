package com.apollo.mira.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apollo.mira.presentation.dashboard.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest

// ============================================================
// LEAK PATTERNS — các pattern gây leak trong Mira và cách fix
//
// File này là reference document, không phải production code.
// Đọc kỹ trước khi review code của người khác hoặc bị hỏi
// trong phỏng vấn về memory management.
// ============================================================

// ─────────────────────────────────────────────────────────────
// PATTERN 1: Collect Flow sai chỗ trong Compose
// ─────────────────────────────────────────────────────────────

/*
❌SAI — collect trong LaunchedEffect không có lifecycle awareness
Flow tiếp tục chạy khi Composable bị removed khỏi composition
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    LaunchedEffect(Unit) {}
        viewModel.uiState.collect { state -> chạy mãi không dừng
            // handle state
        }
    }
}

✅ Đúng - collectAsStateWithLifecycle tự cancel khi Composable off screen
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Lifecycle-aware: pause khi app background, resume khi foreground
}

✅ Đúng - collectLatest trong LaunchedEffect với lifecycle owner
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            // handle event
        }
        // LaunchedEffect tự cancel coroutine khi key (viewModel) thay đổi
        // hoặc khi Composable leave composition
    }
}

// ─────────────────────────────────────────────────────────────
// PATTERN 2: Context leak trong ViewModel
// ───────────────────────

❌ SAI - giữ Activity context trong ViewModel
ViewModel sống lâu hơn Activity (qua rotation)
-> Activity không được garbage collect -> leak toàn bộ View hierarchy
class DashboardViewModel(
    private val context: Context // <- Leak! Activity context
): ViewMoodel

✅ ĐÚNG - Dùng Application context nếu thực sự cần
class DashboardViewModel(
    private val context: Application
): ViewModel

✅ ĐÚNG HƠN - inject qua Hilt với @ApplicationContext
@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context, // Application context
    private val useCase: GetDashboardSummaryUseCase
): ViewModel()

✅ TỐT NHẤT - không inject Context vào ViewModel
Nếu cần string resource: inject StringResourceProvider
Nếu cần format: làm trong Composable, không trong ViewModel


// PATTERN 3: Coroutine không cancel trong ViewModel
❌SAI - dùng GlobalScope thay vì viewModelScope
class DashboardViewModel: ViewModel() {
    fun loadData() {
        GlobalScope.launch { // không bao giờ cancel
            repository.getData() //  chạy mãi kể cả khi ViewModel cleared
        }
    }
}
✅ ĐÚNG - viewModelScope tự cancel khi ViewModel.onCleared()
class DashboardViewModel: ViewModel() {
    fun loadData() {
        viewModelScope.launch { // tự cancel khi user bấm Back
            repository.getData()
        }
    }
}
Trong Mira, DashboardViewModel.kt đã dùng đúng
fun onAddTransactionClick() {
    viewModelScope.launch { _events.emit(...) } // ✅
}

// PATTERN 4: stateIn với SharingStarted
❌ SAI - Eagerly: Flow chạy mãi kể cả khi không có subscriber
val uiState = useCase()
    .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)
    // Database query chạy liên tục kể cả khi app bị background -> drain battery
❌ SAI - Lazily: Flow không cancel sau khi subscriber unsubscribe
val uiState = useCase()
    .stateIn(viewModelScope, SharingStarted.Lazily, UiState.Loading)
// Database query tiếp tục chạy khi user navigate away
✅ ĐÚNG - WhileSubscribed(5000): cân bằng giữa performance và UX
val uiState = useCase()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000)
        initialValue = UiState.Loading
    )
    // Flow active khi có subscriber
    // Giữ active 5s sau khi subscriber unsubscribe (tránh restart khi rotation)
    // Cancel sau 5s -> tiết kiệm battery khi user rời màn hình
Đây là config Mira đang dùng trong DashboardViewModel.kt

// PATTERN 5: Room Flow không cancel
Room Flow là hot observable - nó emit mỗi khi DB thay đổi
Nếu không được cancel đúng cách, Room giữ cursor mở -> memory leak
✅Mira đã handle đúng qua chain:
Room Flow   -> Repository (flowOn IO) -> UseCase (.map, .catch)
            -> ViewModel (.stateIn WhileSubscribed)
            -> Compose (collectAsStateWithLifecycle)
Khi user rời DashboardScreen:
1. collectAsStateWithLifecycle dừng collect
2. sau 5s, stateIn cancel uptream Flow
3. Roow flow bị cancel -> cursor đóng lại
4. Không còn leak
*/