package com.apollo.mira.presentation.add_transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apollo.mira.domain.model.TransactionType
import kotlinx.coroutines.flow.collectLatest
import java.util.logging.Filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AddTransactionEvent.NavigateBack -> onNavigateBack()
                is AddTransactionEvent.ShowError ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Thêm giao dịch") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // ── Loại giao dịch Toggle ───────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { type ->
                        FilterChip(
                            selected = form.transactionType == type,
                            onClick = { viewModel.onTypeToggle(type) },
                            label = {
                                Text(if (type == TransactionType.EXPENSE) "Chi tiêu" else "Thu nhập")
                            }
                        )
                    }
                }
                // ── Số tiền ────────────────────────────────────
                OutlinedTextField(
                    value = "",
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Số tiền") },
                    suffix = { Text("đ") },
                    isError = form.amountError != null,
                    // Hiện error message ngay dưới TextField — Day 3 UiState pattern
                    supportingText = form.amountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                // ── Danh mục ────────────────────────────────────
                CategorySelector(
                    selectedCategory = form.selectedCategory,
                    error = form.categoryError,
                    onCategorySelected = viewModel::onCategorySelected
                )
                // ── Ghi chú ─────────────────────────────────────
                OutlinedTextField(
                    value = form.note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text("Ghi chú (tuỳ chọn") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(Modifier.weight(1f))

                // ── Submit ──────────────────────────────────────
                Button(
                    onClick = viewModel::onSubmit,
                    enabled = !form.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) { }
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Lưu giao dịch")
                }
            }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: String,
    error: String?,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        "Ăn uống", "Di chuyển", "Mua sắm", "Giải trí",
        "Sức khỏe", "Giáo dục", "Nhà ở", "Lương",
        "Đầu tư", "Khác"
    )
    Column {
        Text(
            text = "Danh mục",
            style = MaterialTheme.typography.labelMedium,
            color = if (error != null)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        val rows = categories.chunked(3)
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelected(category) },
                        label = { Text(category, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}