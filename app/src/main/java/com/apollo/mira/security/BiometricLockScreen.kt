package com.apollo.mira.security

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class BiometricViewModel @Inject constructor(
    private val biometricManager: MiraBiometricManager,
    private val securePreferences: SecurePreferences
): ViewModel() {

    // State: authenticated hay chưa
    private val _isAuthenticated = MutableStateFlow(false)

}

@Composable
class BiometricLockScreen(
    onAuthenticated: () -> Unit,
    viewModel: BiometricViewModel = hiltViewModel()
) {

    // Nếu authenticated -> trigger callback, không render lock screen
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) onAuthenticared()
    }

    // Tự động trigger prompt khi màn hình xuất hiện
    LaunchedEffect(Unit) {
        if (viewModel.isBiometricEnabled()) {
            viewModel.
        }
    }


}