package com.apollo.mira.presentation.add_transaction

import app.cash.turbine.test
import com.apollo.mira.domain.usecase.AddTransactionUseCase
import com.apollo.mira.utils.MainDispatcherRule
import com.apollo.mira.utils.TransactionFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val addTransaction = mockk<AddTransactionUseCase>()
    private lateinit var viewModel: AddTransactionViewModel

    @Before
    fun setup() {
        viewModel = AddTransactionViewModel(addTransaction)
    }

    // ── Form state tests ──────────────────────────────────────
    @Test
    fun `init form have empty values and isValid = false`() {
        // Không cần runTest - chỉ check state, không có coroutine
        val form = viewModel.form.value
        assertTrue(form.rawAmount.isEmpty())
        assertTrue(form.selectedCategory.isEmpty())
        assertFalse("New form should not be valid", form.isValid)
    }

    @Test
    fun `onAmountChange with valid amount, no error occur`() {
        viewModel.onAmountChange("150000")
        val form = viewModel.form.value
        assertEquals("150000", form.rawAmount)
        assertNull("No error expected for valid amount", form.amountError)
        assertEquals(150_000.0, form.parsedAmount, 0.001)
    }

    @Test
    fun `onAmountChange with text not a digit-number, exist error with correct message`() {
        viewModel.onAmountChange("abc")
        val form = viewModel.form.value
        assertNotNull("Error expected for non-numeric input", form.amountError)
        assertEquals("Số tiền không hợp lệ", form.amountError)
    }

    @Test
    fun `onAmountChange with 0, exists error message`() {

    }

    @Test
    fun `onAmountChange with negative number, exists error`() {
        viewModel.onAmountChange("-500")
        // "-500" do not parse to positve -> "Số tiền không hợp lệ"
        assertNotNull(viewModel.form.value.amountError)
    }

    @Test
    fun `onAmountChange with empty input, do not display error (user do not typing`() {

    }

    @Test
    fun `onCategorySelected delete category error`() {

    }

    @Test
    fun `onTypeToggle change transactionType`() {

    }

    @Test
    fun `form isValid just become true when amount is valid and category is selected`() {

    }
    // ── Submit tests ──────────────────────────────────────────
    @Test
    fun `onSubmit with empty form, set validate errors and do not call UseCase`() = runTest {
        coEvery { addTransaction(any()) } returns Result.success(TransactionFixtures.anExpense())

        // 1. Thực hiện hành động
        viewModel.onSubmit()
        advanceUntilIdle()

        // 2. Lấy giá trị form hiện tại để kiểm tra
        val form = viewModel.form.value
        println("Log-Debug: amountError là: ${form.amountError}")
        println("Log-Debug: categoryError là: ${form.categoryError}")

        // 3. Kiểm tra xem các lỗi có được set ĐÚNG như mong đợi không (thay vì mong nó Crash)

        assertNotNull("Amount error should be set", form.amountError)
        assertNotNull("Category error should be set", form.categoryError)

        coVerify(exactly = 0) { addTransaction(any()) }
    }

    @Test
    fun `onSubmit success, emit NavigateBack event`() = runTest {
        val saved = TransactionFixtures.anExpense()
        coEvery { addTransaction(any()) } returns Result.success(saved)

        // Fill form hợp lệ
        viewModel.onAmountChange("150000")
        viewModel.onCategorySelected("Ăn uống")

        viewModel.events.test {
            viewModel.onSubmit()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(
                "Expected NavigateBack but got $event",
                event is AddTransactionEvent.NavigateBack
            )
            cancelAndIgnoreRemainingEvents()
        }

    }

}