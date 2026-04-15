package com.apollo.mira.presentation.dashboard

import com.apollo.mira.domain.usecase.GetDashboardSummaryUseCase
import com.apollo.mira.utils.MainDispatcherRule
import io.mockk.mockk
import org.junit.Rule

class DashboardViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

//    private val getDashboardSummary = mockk<GetDashboardSummaryUseCase>()

    private lateinit var viewModel: DashboardViewModel

//    private fun createViewModel(): DashboardViewModel = DashboardViewModel(getDashboardSummary, addTransaction)
}