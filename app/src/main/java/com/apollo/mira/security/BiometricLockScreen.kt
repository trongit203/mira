package com.apollo.mira.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
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
    val uiEvent: SharedFlow<BiometricUiEvent> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            biometricManager.authResult.collect { result ->
                when (result) {
                    BiometricResult.Success -> {
                        _isAuthenticated.value = true
                    }
                    BiometricResult.UserCancelled -> {
                        // User huỷ → vẫn ở lock screen, không làm gì
                    }
                    BiometricResult.LockedOut -> {
                        _uiEvent.emit(BiometricUiEvent.ShowError(
                            "Quá nhiều lần thử. Vui lòng thử lại"
                        ))
                    }
                    is BiometricResult.Error -> {
                        _uiEvent.emit(BiometricUiEvent.ShowError(result.message))
                    }
                }
            }
        }
    }

    fun triggerAuth(activity: FragmentActivity) {
        biometricManager.authenticate(activity)
    }

    fun isBiometricEnabled() = securePreferences.isBiometricEnabled

    sealed class BiometricUiEvent {
        data class ShowError(val message: String): BiometricUiEvent()
    }

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
        if (isAuthenticated) onAuthenticated()
    }

    // Tự động trigger prompt khi màn hình xuất hiện
    LaunchedEffect(Unit) {
        if (viewModel.isBiometricEnabled()) {
            viewModel.triggerAuth(activity)
        }
    }

    // Collect error events
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is BiometricViewModel.BiometricUiEvent.ShowError ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement   = Arrangement.Center,
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Text(
                text  = "Mira",
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Xác thực để tiếp tục",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(48.dp))

            when (viewModel.availability) {
                is BiometricAvailability.Available -> {
                    Button(
                        onClick  = { viewModel.triggerAuth(activity) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dùng vân tay / khuôn mặt")
                    }
                }
                is BiometricAvailability.NoneEnrolled -> {
                    Text(
                        "Bạn chưa thiết lập xác thực sinh trắc học.\n" +
                                "Vào Cài đặt → Bảo mật để thêm vân tay.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    // Fallback: nếu hardware không có → cho qua
                    LaunchedEffect(Unit) { onAuthenticated() }
                }
            }
        }
    }

}