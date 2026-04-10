package com.apollo.mira.presentation.common

sealed class UiState<out T> {
    object Loading: UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(
        val message: String,
        val retryable: Boolean = true,
        val throwable: Throwable? = null
    ) : UiState<Nothing>()

     object Empty: UiState<Nothing>()
}

// ── Extension functions ──────────────────────────────────────
// Dùng trong Compose để viết ngắn hơn

val <T> UiState<T>.isLoading get() = this is UiState.Loading
val <T> UiState<T>.isEmpty get() = this is UiState.Empty

// Helper extensions để dùng trong Compose
fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data

fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Loading -> UiState.Loading
    is UiState.Empty -> UiState.Empty
    is UiState.Success -> UiState.Success(transform(data))
    is UiState.Error -> UiState.Error(message, retryable, throwable)
}