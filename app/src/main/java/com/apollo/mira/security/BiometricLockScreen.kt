package com.apollo.mira.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class BiometricViewModel @Inject constructor(
    private val biometricManager: MiraBiometricManager,
    private val securePreferences: SecurePreferences
): ViewModel() {

    // State: authenticated hay chưa
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    val availability = biometricManager.checkBiometricAvailability()

    // Event từ BiometricPrompt callback
    private val _uiEvent = MutableSharedFlow<BiometricUiEvent>()

}

@Composable
fun BiometricLockScreen(
    onAuthenticated: () -> Unit,
    viewModel: BiometricViewModel = hiltViewModel()
) {

    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val activity = LocalContext.current as FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }

    // Nếu authenticated -> trigger callback, không render lock screen
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) onAuthenticared()
    }

    // Tự động trigger prompt khi màn hình xuất hiện
    LaunchedEffect(Unit) {
        if (viewModel.isBiometricEnabled()) {
            viewModel.triggerAuth(activity)
        }
    }

    // Collect error events
    LaunchedEffect(viewModel) {

    }

}