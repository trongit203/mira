package com.apollo.mira.domain.usecase

import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.utils.FakeTransactionRepository
import com.apollo.mira.utils.MainDispatcherRule
import com.apollo.mira.utils.TransactionFixtures
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.expect

// ============================================================
// ADD TRANSACTION USE CASE TEST

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionUseCaseTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTransactionRepository
    private lateinit var useCase: AddTransactionUseCase

    @Before
    fun setup() {
        fakeRepository = FakeTransactionRepository()
        useCase = AddTransactionUseCase(fakeRepository)
    }

//    Validate tests - business rule của UseCase

    @Test
    fun `valid transaction being saved successfully`() = runTest {
        val transaction = TransactionFixtures.anExpense()
        val result = useCase(transaction)

        assertTrue("Expected success", result.isSuccess)
        // Verify ID được assign từ DB (không còn là 0)
        assertTrue(result.getOrThrow().id > 0)
    }

    @Test
    fun `amount = 0 is rejected with the correct message`() = runTest {
        val invalid = TransactionFixtures.aTransaction(amount = 0.0)
        val result = useCase(invalid)

        assertTrue("Expect failure for zero amount", result.isFailure)
        assertEquals(
            "Số tiền phải lớn hơn 0",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `negative amount is rejected`() = runTest {
        val invalid = TransactionFixtures.aTransaction(amount = -500.0)
        val ex = assertFailsWith<IllegalArgumentException> {
            useCase(invalid)
        }
        assertEquals("Số tiền phải lớn hơn 0", ex.message)
    }

    @Test
    fun `empty category is rejected`() = runTest {
        val invalid = TransactionFixtures.aTransaction(category = "")

        val ex = assertFailsWith<IllegalArgumentException>{
            useCase(invalid)
        }
        assertEquals("Phải chọn danh mục", ex.message)
    }

    @Test
    fun `category only have whitespace is rejected`() = runTest {
        val invalid = TransactionFixtures.aTransaction(category = "    ")
        var result: Result<Transaction>? = null
        assertFailsWith <IllegalArgumentException>{
            result = useCase(invalid)
        }
        assertTrue(result == null || result.isFailure)
    }

    @Test
    fun `valid transaction only call repository dot addTransaction 1 time`() = runTest {
//        spy: wrap FakeRepository, giữ behaviour thật nhưng record calls
        val spyRepo = spyk(fakeRepository)
        val useCaseWithSpy = AddTransactionUseCase(spyRepo)

        val transaction = TransactionFixtures.anExpense()
        useCaseWithSpy(transaction)

        // Verify: addTransaction chỉ được gọi đúng 1 lần
        coVerify(exactly = 1) { spyRepo.addTransaction(any())}
    }

    @Test
    fun `invalid transaction DO NOT call repository dot addTransaction`() = runTest {
        val spyRepo = spyk(fakeRepository)
        val useCaseWithSpy = AddTransactionUseCase(spyRepo)

        // Invalid transaction — validation fail trước khi đến repo

        var result: Result<Transaction>? = null

        assertFailsWith <IllegalArgumentException> {
            result = useCaseWithSpy(TransactionFixtures.aTransaction(amount = 0.0))
        }

        // Verify: repository KHÔNG được gọi khi validation fail
        coVerify(exactly = 0) { spyRepo.addTransaction(any()) }
        assertTrue(result == null || result.isFailure)
    }

    @Test
    fun `when repository fail, Result dot failure is returned`() = runTest {
        fakeRepository.shouldThrowError = true
        fakeRepository.errorMessage     = "Database full"

        val result = useCase(TransactionFixtures.anExpense())

        assertTrue(result.isFailure)
        assertEquals("Database full", result.exceptionOrNull()?.message)
    }
}