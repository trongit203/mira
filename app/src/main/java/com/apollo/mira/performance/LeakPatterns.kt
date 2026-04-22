package com.apollo.mira.performance

import androidx.compose.runtime.Composable
import com.apollo.mira.presentation.dashboard.DashboardViewModel

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

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {

}