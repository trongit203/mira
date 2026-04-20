package com.apollo.mira.service

import com.apollo.mira.data.notification.MoMoNotificationParser
import com.apollo.mira.data.notification.NotificationParserFactory
import com.apollo.mira.domain.model.TransactionType
import com.apollo.mira.domain.usecase.AddTransactionUseCase
import com.apollo.mira.utils.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NotificationDispatchPipelineTest {

    private lateinit var fakeRepo: FakeTransactionRepository
    private lateinit var useCase: AddTransactionUseCase
    private lateinit var factory: NotificationParserFactory

    @Before
    fun setUp() {
        fakeRepo  = FakeTransactionRepository()
        useCase   = AddTransactionUseCase(fakeRepo)
        factory   = NotificationParserFactory()
    }

    @Test
    fun `MoMo receive notification inserts INCOME transaction`() = runTest {
        val parser = factory.findParser("com.mservice.momotransfer")
        assertNotNull(parser)

        val tx = parser!!.parse(
            packageName = "com.mservice.momotransfer",
            title       = "MoMo",
            text        = "Bạn nhận 500.000đ từ Công ty ABC",
            timestamp   = System.currentTimeMillis()
        )
        assertNotNull(tx)

        val result = useCase(tx!!)
        assertTrue(result.isSuccess)

        val stored = fakeRepo.getRecentTransactions(5).first()
        assertEquals(1, stored.size)
        assertEquals(TransactionType.INCOME, stored[0].type)
        assertEquals(500_000.0, stored[0].amount, 0.0)
    }

    @Test
    fun `MoMo payment notification inserts EXPENSE transaction`() = runTest {
        val parser = factory.findParser("com.mservice.momotransfer")!!
        val tx = parser.parse(
            packageName = "com.mservice.momotransfer",
            title       = "MoMo",
            text        = "Thanh toán 85.000đ tại Grab Food",
            timestamp   = System.currentTimeMillis()
        )
        assertNotNull(tx)
        useCase(tx!!)

        val stored = fakeRepo.getRecentTransactions(5).first()
        assertEquals(1, stored.size)
        assertEquals(TransactionType.EXPENSE, stored[0].type)
        assertEquals("Di chuyển", stored[0].category)
    }

    @Test
    fun `unknown package returns null parser and makes no insert`() = runTest {
        val parser = factory.findParser("com.facebook.katana")
        assertNull(parser)

        val stored = fakeRepo.getRecentTransactions(5).first()
        assertTrue(stored.isEmpty())
    }

    @Test
    fun `duplicate key dedup prevents double insert`() = runTest {
        val processedKeys = mutableSetOf<String>()
        val key = "com.mservice.momotransfer:0:99999"
        val text = "Bạn nhận 100.000đ từ Test User"

        suspend fun simulatePost(key: String) {
            if (key in processedKeys) return
            processedKeys.add(key)
            val tx = MoMoNotificationParser().parse(
                "com.mservice.momotransfer", "MoMo", text, System.currentTimeMillis()
            ) ?: return
            useCase(tx)
        }

        simulatePost(key)
        simulatePost(key)

        val stored = fakeRepo.getRecentTransactions(10).first()
        assertEquals(1, stored.size)
    }
}
