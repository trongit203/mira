package com.apollo.mira.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.mira.security.BiometricAvailability
import com.apollo.mira.security.BiometricResult
import com.apollo.mira.security.MiraBiometricManager
import com.apollo.mira.security.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val securePreferences: SecurePreferences,
    private val biometricManager: MiraBiometricManager
): ViewModel() {
   private val _isBiometricEnabled = MutableStateFlow(
       securePreferences.isBiometricEnabled
   )

    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    val biometricAvailability = biometricManager.checkBiometricAvailability()

    private val _events = MutableSharedFlow<SecuritySettingsEvent>()
    val events: SharedFlow<SecuritySettingsEvent> = _events.asSharedFlow()

    init {
        // Lắng nghe kết quả xác thực từ BiometricPrompt
        viewModelScope.launch {
            biometricManager.authResult.collect { result ->
                println("log-70 - $result")
                when (result) {
                    is BiometricResult.Success -> {
                        // Xác thực thành công → lưu setting
                        val newValue = !_isBiometricEnabled.value
                        securePreferences.isBiometricEnabled = newValue
                        _isBiometricEnabled.value = newValue
                        val msg = if (newValue) "Đã bật khóa vân tay" else "Đã tắt khóa vân tay"

                        _events.emit(SecuritySettingsEvent.ShowSnackbar(msg))
                    }
                    is BiometricResult.UserCancelled -> {
                        // Không làm gì — toggle không thay đổi
                    }
                    else -> {
                        _events.emit(SecuritySettingsEvent.ShowSnackbar("Xác thực thất bại. Vui lòng thử lại"))
                    }
                }
            }
        }
    }

    // User bấm toggle -> yêu cầu xác thực trước khi đổi setting
    fun onBiometricToggle(activity: FragmentActivity) {
        biometricManager.authenticate(activity)
    }

    sealed class SecuritySettingsEvent {
        data class ShowSnackbar(val message: String): SecuritySettingsEvent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SecuritySettingsViewModel = hiltViewModel()
) {
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current as FragmentActivity

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SecuritySettingsViewModel.SecuritySettingsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Bảo mật") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            when (viewModel.biometricAvailability) {
                is BiometricAvailability.Available -> {
                    BiometricToggleItem(
                        isEnabled = isBiometricEnabled,
                        onToggle = { viewModel.onBiometricToggle(activity) }
                    )
                }
                is BiometricAvailability.NoneEnrolled -> {
                    ListItem(
                        headlineContent = { Text("Khóa bằng vây tay") },
                        supportingContent = {
                            Text("Bạn chưa thiết lập vân tay. " + "Vào cài đặt hệ thống -> Bảo mật để thêm.", color = MaterialTheme.colorScheme.error)
                        },
                        trailingContent = {
                            Switch(enabled = false, checked = false, onCheckedChange = {})
                        }
                    )
                }
                is BiometricAvailability.NoHardware -> {
                    ListItem(
                        headlineContent = { Text("Khóa bằng vân tay") },
                        supportingContent = {
                            Text("Thiết bị không hỗ trợ tính năng này")
                        },
                        trailingContent = {
                            Switch(enabled = false, checked = false, onCheckedChange = {})
                        }

                    )
                }
                else -> {
                    /* HardwareUnavailable / Unknown */
                }
            }
            HorizontalDivider()

            // ── Token info (read-only) ────────────────────────
            ListItem(
                headlineContent   = { Text("Phiên đăng nhập") },
                supportingContent = {
                    Text(
                        "Token được mã hoá bằng AES-256-GCM\n" +
                                "và lưu trong Android Keystore."
                    )
                }
            )
        }
    }

}

@Composable
private fun BiometricToggleItem(
    isEnabled: Boolean,
    onToggle: () -> Unit,
) {
    ListItem(
        headlineContent = { Text("Khóa bằng vân tay / khuôn mặt") },
        supportingContent = {
            Text(
                if (isEnabled) "Bật - App yêu cầu xác thực khi mở"
                else "Tắt - App mở trực tiếp"
            )
        },
        trailingContent = {
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    )
}