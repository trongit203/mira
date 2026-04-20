package com.apollo.mira.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue // Nếu sau này bạn dùng 'var ... by remember'
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.presentation.common.UiState
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.apollo.mira.TestTags
import com.apollo.mira.domain.model.DashboardSummary
import com.apollo.mira.domain.model.TransactionType
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.apollo.mira.utils.NotificationPermissionUtils
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Locale

// ============================================================
// DASHBOARD SCREEN - Compose UI
// Nguyen tac:
// - collectedAsStateWithLifecycle: chỉ collect khi app foreground
// - SharedFlow collect trong LaunchedEffect (không phải collectAsState)
// - when(uiState) xử lý tất cả state - không if/else
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    // collectAsStateWithLifecycle: lifecycle-aware, chỉ collect khi STARTED
    // cần dependency: androidx.lifecycle:lifecycle-runtime-compose
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner    = LocalLifecycleOwner.current

    // SharedFlow -> LaunchedEffect (không dùng cbodel thay đổi
    LaunchedEffect(viewModel.events, lifecycleOwner) {
        viewModel.events.collectLatest { event ->
            when (event) { 
                is DashboardEvent.ShowSnackbar -> 
                    snackbarHostState.showSnackbar(event.message)

                DashboardEvent.NavigateToAddTransaction ->
                    onNavigateToAddTransaction()

                is DashboardEvent.NavigateToDetail ->
                    onNavigateToDetail(event.transactionId)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mira") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cài đặt bảo mật"
                        )
                    }
                }
            )
        },
        floatingActionButton = { 
            FloatingActionButton(
                onClick = viewModel::onAddTransactionClick,
                modifier = Modifier.testTag(TestTags.FAB_ADD))
            { Text("+") }
        }   
    ) { paddingValues ->
        val context = LocalContext.current
        Column(modifier = Modifier.padding(paddingValues)) {
            if (!NotificationPermissionUtils.isNotificationAccessGranted(context)) {
                NotificationPermissionBanner()
            }
            when (val state = uiState) {
            is UiState.Loading -> LoadingContent(Modifier.padding(paddingValues))

            is UiState.Empty ->
                EmptyDashboardContent(
                    onAddClick = viewModel::onAddTransactionClick,
                    modifier = Modifier.padding(paddingValues)
                )

            is UiState.Error -> 
                ErrorContent(
                    message = state.message,
                    // retry chỉ show nếu retryable = true
                    // ViewModel tự reload vì StateFlow vẫn active
                    modifier = Modifier.padding(paddingValues)
                )

            is UiState.Success ->
                DashboardContent(
                    summary = state.data,
                    onTransactionClick = viewModel::onTransactionClick,
                    modifier = Modifier
                )
        }
        }
     }
}

@Composable
private fun NotificationPermissionBanner() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cho phép đọc thông báo để tự động ghi giao dịch từ MoMo, ZaloPay và ngân hàng.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }) {
                Text("Cấp quyền")
            }
        }
    }
}

// Sub-composables

@Composable
private fun DashboardContent(
    summary: DashboardSummary,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            BalanceCard(
                balance = summary.netBalance,
                income = summary.totalIncome,
                expense = summary.totalExpense
            )
        }
        item {
            Text(
                text = "Giao dịch gần đây",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(
            items = summary.recentTransactions,
            key = { it.id } // key quan trong cho performance LazyColumn
        ) { transaction ->
            TransactionItem(
                transaction = transaction,
                onClick = { onTransactionClick(transaction.id) }
            )
        }
    }
}

@Composable
private fun BalanceCard(
    balance: Double,
    income: Double,
    expense: Double
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Số dư hiện tại", style = MaterialTheme.typography.labelMedium)
            Text(
                text = formatCurrency(balance),
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Thu nhập", style = MaterialTheme.typography.labelSmall)
                    Text(formatCurrency(income), color = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Chi tiêu", style = MaterialTheme.typography.labelSmall)
                    Text(formatCurrency(expense), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(transaction.category) },
        supportingContent = if (transaction.note.isNotEmpty()) {
            { Text(transaction.note) }
        } else {
            null
        },
        trailingContent = {
            val isIncome = transaction.type === TransactionType.INCOME
            Text(
                text = "${if (isIncome) "+" else "-"}${formatCurrency(transaction.amount)}",
                color = if (isIncome)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Có lỗi xảy ra", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatCurrency(amount: Double): String = 
    amount.let { raw ->
        NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            .format(raw)
            .replace("₫", "đ")
    }

@Composable
private fun EmptyDashboardContent(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
     ) {
        Text(
            text = "Chưa có giao dịch nào",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Thêm giao dịch đầu tiên của bạn",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Text("Thêm giao dịch")
        }
    }
}