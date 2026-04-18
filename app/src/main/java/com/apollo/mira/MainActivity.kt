package com.apollo.mira

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apollo.mira.presentation.add_transaction.AddTransactionScreen
import com.apollo.mira.presentation.dashboard.DashboardScreen
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Routes.DASHBOARD
                ) {
                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            onNavigateToAddTransaction = {
                                navController.navigate(Routes.ADD_TRANSACTION)
                            },
                            onNavigateToDetail = { id -> /*TODO*/ },
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

//                    SecuritySettingsScreen được wire vào đây
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

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_TRANSACTION = "add_transaction"
    const val SECURITY_SETTINGS = "security_settings"
}