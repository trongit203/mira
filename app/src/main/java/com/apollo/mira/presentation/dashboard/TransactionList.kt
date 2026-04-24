package com.apollo.mira.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.model.TransactionType

// ============================================================
// TRANSACTION LIST - tối ưu recomposition
// Vấn đề: mỗi lần DashboardScreen recompose (vd: state thay đổi), toàn bộ
// danh sách có thể recompose theo dù data không đổi. Cần tách nhỏ
// và dùng các kỹ thuật ổn định hoá
// ============================================================
// -- Kỹ thuật 1: key trong LazyColumn --
// Compose dùng key để biết item nào thay đổi, thay vì redraw hết
@Composable
fun OptimizedTransactionList(
    transactions: List<Transaction>,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            BalanceSummaryCard(transactions = transactions, modifier = modifier)
        }
        item {
            Text(
                text = "Giao dịch gần đây",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(
            items = transactions,
            key = { transaction -> transaction.id }
            // ✅ key: Compose chỉ recompose item có id thay đổi
            // ❌ không có keyL thêm 1 item đầu list -> recompose toàn bộ
        ) { transaction ->
            // Mỗi item là Composable riêng biệt
            TransactionListItem(
                transaction = transaction,
                onTransactionClick = onTransactionClick
            )
        }
    }
}

// -- Kỹ thuật 2: tách Composable nhỏ --
// Composable càng nhỏ, scope recomposition càng hẹp
@Composable
fun TransactionListItem(
    transaction: Transaction,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Tính toán bên ngoài composition - chỉ recalculate khi input thay đổi
    val isIncome = transaction.type == TransactionType.INCOME
    val amountDisplay = remember(transaction.amount, isIncome) {
        // remember: cache kết quả, chỉ recalculate khi dependency thay đổi
        val sign = if (isIncome) "+" else "-"
        "$sign${"%,.0f".format(transaction.amount)} đ"
    }

    ListItem(
        headlineContent = { Text(transaction.category) },
        supportingContent = { Text(transaction.note) },
        trailingContent = {
            Text(
                text = amountDisplay,
                color = if (isIncome)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error

            )
        },
        modifier = modifier.clickable
        {onTransactionClick(transaction.id)}
    )
    HorizontalDivider()
}

// -- Kỹ thuât 3: lamdas ổn định --
// Lamda capture mới mỗi recompose -> Compose nghĩ param thay đổi

/*
❌ Sai - lamda mới mỗi lần recompose DashboardScreen
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    TransactionList(
        onTransactionClick = { id ->        // ← lambda mới mỗi lần!
            viewModel.onTransactionClick(id)
        }
    )
}

✅ Đúng - method reference ổn định, không tạo object mới
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    TransactionList(
        onTransactionClick = viewModel::onTransactionClick // <- stable
    )
}

✅ Đúng - rememberUpdatedState cho lamda phức tạp hơn
fun DashboardScreen(onItemClick: (Long) -> Unit) {
    val stableOnClick by rememberUpdatedState(onItemClick)
    TransactionList(
        onTransactionClick = { id -> stableOnClick(id }
    )
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {

}
* */
// -- Kỹ thuật 4: derivedStateOf --
// Tính toán từ state khác, chỉ trigger recompose khi kết quả thay đổi
@Composable
fun BalanceSummaryCard(
    transactions: List<Transaction>,
    modifier: Modifier
) {
    // ❌ SAI - tính lại mỗi lần recompose mặc dù transactions không đổi
    // val totalIncome = transaction.filter { it.type == INCOME }.sumOf { it.amount }
    //
    // ✅ ĐÚNG - derivedStateOf: chỉ recalculate khi transactions thay đổi
    // Nếu recompose do lý do khác -> dùng cached value
    val totalIncome by remember {
        derivedStateOf {
            transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
        }
    }

    val totalExpense by remember {
        derivedStateOf {
            transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
        }
    }

    // netBalance chỉ recompose khi totalIncome hoặc totalExpense thay đổi
    val netBalance by remember {
        derivedStateOf { totalIncome - totalExpense }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Số dư", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "%,.0f đ".format(netBalance),
                style = MaterialTheme.typography.headlineLarge
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Thu", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "%,.0f đ".format(totalIncome),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Chi", style = MaterialTheme.typography.labelSmall)
                    Text("%,.0f đ".format(totalExpense),
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}