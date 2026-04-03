package com.apollo.mira.presentation.common

sealed class UiState<out T> {
    object Loading: UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(
        val message: String,
        val retryable: Boolean = true
    ) : UiState<Nothing>()
}

// Helper extensions để dùng trong Compose
fun <T> UiState<T>.isLoading() = this is UiState.Loading
fun <T> UiState<T>.dataOrNull() = (this as? UiState.Success)?.data