package com.apollo.mira.presentation.dashboard

import androidx.compose.material3.Scaffold

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifeCycle()

    val snackbarHostState = remember { SnackBarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel.events, lifecycleOwner) {
        viewModel.events
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collect { event ->
                when (event) {
//                    TODO: Trong
                }

            }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> LoadingScreen()
            is DashboardUiState.Error -> ErrorScreen(state.message, state.retryAction)
            is DashboardUiState.Success -> DashboardContent(state, padding)
        }
    }
}