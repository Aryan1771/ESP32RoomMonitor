package com.example.roommonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roommonitor.data.ProvisioningRepository
import com.example.roommonitor.data.ProvisioningStatusDto
import com.example.roommonitor.util.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProvisioningUiState(
    val isCheckingDevice: Boolean = false,
    val isScanningNetworks: Boolean = false,
    val isSubmitting: Boolean = false,
    val deviceStatus: ProvisioningStatusDto? = null,
    val availableNetworks: List<String> = emptyList(),
    val ssidInput: String = "",
    val passwordInput: String = "",
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class ProvisioningViewModel : ViewModel() {
    private val repository = ProvisioningRepository(NetworkModule.provisioningApi)

    private val _uiState = MutableStateFlow(ProvisioningUiState())
    val uiState: StateFlow<ProvisioningUiState> = _uiState.asStateFlow()

    fun refreshProvisioningStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingDevice = true,
                errorMessage = null,
                infoMessage = null
            )

            runCatching { repository.fetchStatus() }
                .onSuccess { status ->
                    _uiState.value = _uiState.value.copy(
                        isCheckingDevice = false,
                        deviceStatus = status,
                        infoMessage = "ESP32 setup hotspot reached successfully.",
                        errorMessage = null
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isCheckingDevice = false,
                        deviceStatus = null,
                        errorMessage = throwable.message ?: "Could not reach the ESP32 setup hotspot."
                    )
                }
        }
    }

    fun scanNetworks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanningNetworks = true,
                errorMessage = null,
                infoMessage = null
            )

            runCatching { repository.scanNetworks() }
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isScanningNetworks = false,
                        availableNetworks = response.networks,
                        infoMessage = if (response.networks.isEmpty()) {
                            "No nearby Wi-Fi names were returned. You can still type one manually."
                        } else {
                            "Tap a Wi-Fi name to fill the SSID field."
                        },
                        errorMessage = if (response.ok) null else response.message
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isScanningNetworks = false,
                        errorMessage = throwable.message ?: "Could not scan Wi-Fi networks from the ESP32."
                    )
                }
        }
    }

    fun updateSsid(value: String) {
        _uiState.value = _uiState.value.copy(ssidInput = value)
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(passwordInput = value)
    }

    fun chooseNetwork(ssid: String) {
        _uiState.value = _uiState.value.copy(ssidInput = ssid)
    }

    fun submitCredentials() {
        val ssid = _uiState.value.ssidInput.trim()
        val password = _uiState.value.passwordInput

        if (ssid.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Enter or select an SSID first."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSubmitting = true,
                errorMessage = null,
                infoMessage = null
            )

            runCatching { repository.configureWifi(ssid, password) }
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        passwordInput = "",
                        infoMessage = response.message
                            ?: "Credentials sent. The ESP32 should restart and join your Wi-Fi.",
                        errorMessage = if (response.ok) null else response.message
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Could not send Wi-Fi credentials to the ESP32."
                    )
                }
        }
    }
}
