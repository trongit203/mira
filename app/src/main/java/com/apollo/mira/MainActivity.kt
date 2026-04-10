package com.apollo.mira

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.apollo.mira.presentation.dashboard.DashboardScreen
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DashboardScreen(
                    onNavigateToAddTransaction = { /*TODO: */ },
                    onNavigateToDetail = { id -> /**/ }
                )
            }

        }
    }
}