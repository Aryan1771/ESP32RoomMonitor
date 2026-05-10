package com.example.roommonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roommonitor.data.RoomStatusDto
import com.example.roommonitor.data.RoomStatusRepository
import com.example.roommonitor.util.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoomStatusUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val status: RoomStatusDto? = null,
    val lightHistory: List<Int> = emptyList(),
    val errorMessage: String? = null
)

class RoomStatusViewModel : ViewModel() {
    private val repository = RoomStatusRepository(NetworkModule.roomStatusApi)

    private val _uiState = MutableStateFlow(RoomStatusUiState(isLoading = true))
    val uiState: StateFlow<RoomStatusUiState> = _uiState.asStateFlow()

    init {
        refresh(initialLoad = true)
    }

    fun refresh(initialLoad: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = initialLoad,
                isRefreshing = !initialLoad,
                errorMessage = null
            )

            runCatching { repository.fetchStatus() }
                .onSuccess { status ->
                    val nextHistory = buildList {
                        addAll(_uiState.value.lightHistory.takeLast(11))
                        status.lightPercent?.let { add(it) }
                    }

                    _uiState.value = RoomStatusUiState(
                        isLoading = false,
                        isRefreshing = false,
                        status = status,
                        lightHistory = nextHistory
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = RoomStatusUiState(
                        isLoading = false,
                        isRefreshing = false,
                        status = _uiState.value.status,
                        lightHistory = _uiState.value.lightHistory,
                        errorMessage = throwable.message ?: "Unknown error"
                    )
                }
        }
    }
}
