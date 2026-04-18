package com.apollo.mira

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apollo.mira.presentation.add_transaction.AddTransactionScreen
import com.apollo.mira.presentation.dashboard.DashboardScreen
import com.apollo.mira.security.BiometricLockScreen
import com.apollo.mira.security.SecurePreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {

    @Inject
    lateinit var securePreferences: SecurePreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                var isAuthenticated by rememberSaveable {
                    mutableStateOf(!securePreferences.isBiometricEnabled)
                }

                if (!isAuthenticated) {
                    BiometricLockScreen(
                        onAuthenticated = { isAuthenticated = true }
                    )
                } else {
                    // ── MAIN APP ──────────────────────────────────
                    // Chỉ hiện sau khi đã auth
                    val navController = rememberNavController()

                    NavHost(
                        navController    = navController,
                        startDestination = Routes.DASHBOARD
                    ) {
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                onNavigateToAddTransaction = {
                                    navController.navigate(Routes.ADD_TRANSACTION)
                                },
                                onNavigateToDetail = {
                                    // Detail screen — implement sau
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Routes.SECURITY_SETTINGS)
                                }
                            )
                        }

                        composable(Routes.ADD_TRANSACTION) {
                            AddTransactionScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // ← SecuritySettingsScreen được wire vào đây
                        composable(Routes.SECURITY_SETTINGS) {
                            com.apollo.mira.presentation.settings.SecuritySettingsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }

        }
    }
}

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_TRANSACTION = "add_transaction"
    const val SECURITY_SETTINGS = "security_settings"
}