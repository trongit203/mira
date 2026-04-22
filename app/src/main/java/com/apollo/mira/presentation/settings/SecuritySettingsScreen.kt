package com.apollo.mira.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.mira.security.BiometricAvailability
import com.apollo.mira.security.BiometricResult
import com.apollo.mira.security.MiraBiometricManager
import com.apollo.mira.security.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================
// SECURITY SETTINGS — màn hình Settings > Bảo mật
//
// Đây là nơi SecurePreferences.isBiometricEnabled được SET.
// Flow:
// 1. User bật toggle "Khoá bằng vân tay"
// 2. Trigger BiometricPrompt để xác nhận
// 3. Xác thực thành công → lưu isBiometricEnabled = true
// 4. Lần sau mở app → MainActivity đọc → hiện BiometricLockScreen
//
// Không cho phép bật mà không verify trước — tránh user bật
// nhầm rồi bị khoá ngoài app chính mình
// ============================================================

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val securePreferences: SecurePreferences,
    private val biometricManager: MiraBiometricManager
) : ViewModel() {

    // State hiện tại từ SecurePreferences
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
                when (result) {
                    is BiometricResult.Success -> {
                        // Xác thực thành công → lưu setting
                        val newValue = !_isBiometricEnabled.value
                        securePreferences.isBiometricEnabled = newValue
                        _isBiometricEnabled.value = newValue

                        val msg = if (newValue) "Đã bật khoá vân tay"
                                  else "Đã tắt khoá vân tay"
                        _events.emit(SecuritySettingsEvent.ShowSnackbar(msg))
                    }
                    is BiometricResult.UserCancelled -> {
                        // Không làm gì — toggle không thay đổi
                    }
                    else -> {
                        _events.emit(SecuritySettingsEvent.ShowSnackbar(
                            "Xác thực thất bại. Vui lòng thử lại."
                        ))
                    }
                }
            }
        }
    }

    // User bấm toggle → yêu cầu xác thực trước khi đổi setting
    fun onBiometricToggle(activity: FragmentActivity) {
        biometricManager.authenticate(activity)
    }

    sealed class SecuritySettingsEvent {
        data class ShowSnackbar(val message: String) : SecuritySettingsEvent()
    }
}

// ── Compose UI ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SecuritySettingsViewModel = hiltViewModel()
) {
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val snackbarHostState  = remember { SnackbarHostState() }
    val activity           = LocalContext.current as FragmentActivity

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SecuritySettingsViewModel.SecuritySettingsEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message)
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
                        Icon(Icons.Default.ArrowBack, "Quay lại")
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

            // ── Biometric toggle ──────────────────────────────
            when (viewModel.biometricAvailability) {
                is BiometricAvailability.Available -> {
                    BiometricToggleItem(
                        isEnabled = isBiometricEnabled,
                        onToggle  = { viewModel.onBiometricToggle(activity) }
                    )
                }
                is BiometricAvailability.NoneEnrolled -> {
                    // Thiết bị có hardware nhưng chưa đăng ký vân tay
                    ListItem(
                        headlineContent   = { Text("Khoá bằng vân tay") },
                        supportingContent = {
                            Text(
                                "Bạn chưa thiết lập vân tay. " +
                                "Vào Cài đặt hệ thống → Bảo mật để thêm.",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = {
                            Switch(enabled = false, checked = false, onCheckedChange = {})
                        }
                    )
                }
                is BiometricAvailability.NoHardware -> {
                    ListItem(
                        headlineContent   = { Text("Khoá bằng vân tay") },
                        supportingContent = {
                            Text("Thiết bị không hỗ trợ tính năng này.")
                        },
                        trailingContent = {
                            Switch(enabled = false, checked = false, onCheckedChange = {})
                        }
                    )
                }
                else -> { /* HardwareUnavailable / Unknown — không hiện */ }
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
    onToggle: () -> Unit
) {
    ListItem(
        headlineContent   = { Text("Khoá bằng vân tay / khuôn mặt") },
        supportingContent = {
            Text(
                if (isEnabled) "Bật — App yêu cầu xác thực khi mở"
                else "Tắt — App mở trực tiếp"
            )
        },
        trailingContent = {
            Switch(
                checked         = isEnabled,
                // onCheckedChange trigger biometric verify trước khi đổi
                onCheckedChange = { onToggle() }
            )
        }
    )
}
