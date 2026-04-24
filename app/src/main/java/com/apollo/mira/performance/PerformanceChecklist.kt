package com.mira.performance

// ============================================================
// PERFORMANCE CHECKLIST — những gì đã apply trong Mira
// Dùng khi phỏng vấn hỏi "bạn optimize performance thế nào?"
// ============================================================

/*
─────────────────────────────────────────────────────────────
MEMORY — đã implement
─────────────────────────────────────────────────────────────

✅ viewModelScope thay vì GlobalScope
   File: tất cả ViewModel
   Effect: coroutine tự cancel khi ViewModel cleared → không leak

✅ collectAsStateWithLifecycle thay vì collectAsState
   File: DashboardScreen.kt, AddTransactionScreen.kt
   Effect: chỉ collect khi app foreground → tiết kiệm CPU + battery

✅ SharingStarted.WhileSubscribed(5000) trong stateIn
   File: DashboardViewModel.kt
   Effect: Room query cancel sau 5s khi không có subscriber
           → không giữ DB cursor mở vô thời hạn

✅ flowOn(Dispatchers.IO) trong Repository
   File: TransactionRepositoryImpl.kt
   Effect: DB query chạy IO thread, không block Main thread

✅ @ApplicationContext thay vì Activity context
   File: SecurePreferences.kt, MiraBiometricManager.kt
   Effect: không leak Activity

─────────────────────────────────────────────────────────────
COMPOSE RECOMPOSITION — đã implement
─────────────────────────────────────────────────────────────

✅ key trong LazyColumn
   File: DashboardScreen.kt, TransactionList.kt
   Effect: thêm 1 item → chỉ recompose item mới, không recompose hết

✅ Method reference thay vì lambda mới
   File: DashboardScreen.kt
   Effect: viewModel::onTransactionClick stable → không trigger recompose
           khi parent recompose

✅ remember + derivedStateOf cho tính toán tốn CPU
   File: TransactionList.kt (BalanceSummaryCard)
   Effect: tính income/expense chỉ khi transactions thay đổi
           không tính lại mỗi recompose

✅ remember cho formatted string
   File: TransactionList.kt (TransactionListItem)
   Effect: format currency chỉ khi amount thay đổi

─────────────────────────────────────────────────────────────
DATABASE — đã implement
─────────────────────────────────────────────────────────────

✅ LIMIT trong DAO query
   File: TransactionDao.kt
   Code: SELECT * ... LIMIT :limit
   Effect: Dashboard chỉ load 20 records gần nhất
           không load toàn bộ history → tiết kiệm memory

✅ Soft delete thay vì hard delete
   File: TransactionDao.kt
   Code: UPDATE SET isDeleted = 1
   Effect: không trigger full table reindex
           UX tốt hơn (có thể undo)

✅ isDeleted = 0 filter trong query
   File: TransactionDao.kt
   Code: WHERE isDeleted = 0
   Effect: Room chỉ observe row chưa xoá
           emit ít hơn khi có thay đổi không liên quan

─────────────────────────────────────────────────────────────
CÁCH TRẢ LỜI PHỎNG VẤN
─────────────────────────────────────────────────────────────

Câu hỏi: "Bạn handle performance thế nào trong project Android?"

Trả lời: "Tôi tiếp cận theo 3 tầng:

Tầng memory: dùng viewModelScope để coroutine tự cancel,
collectAsStateWithLifecycle thay vì collectAsState để chỉ
collect khi app foreground, và WhileSubscribed(5000) cho
stateIn để Room query không giữ cursor mở khi không cần.

Tầng Compose: key trong LazyColumn để chỉ recompose item
thay đổi, derivedStateOf để cache tính toán tốn CPU, và
method reference thay vì lambda để tránh recompose không cần.

Tầng đo đạc: tôi dùng Android Profiler Memory tab để detect
leak sau khi rotate nhiều lần, Layout Inspector để đếm
recomposition count, và LeakCanary trong debug build để
tự động phát hiện leak mà không cần manual check."
*/
