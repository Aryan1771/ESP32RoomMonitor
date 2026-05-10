package com.example.roommonitor.data

data class ProvisioningStatusDto(
    val ok: Boolean = false,
    val mode: String? = null,
    val deviceId: String? = null,
    val apSsid: String? = null,
    val apPasswordRequired: Boolean = false,
    val provisioningUrl: String? = null
)

data class ProvisioningScanDto(
    val ok: Boolean = false,
    val networks: List<String> = emptyList(),
    val message: String? = null
)

data class ProvisioningRequestDto(
    val ssid: String,
    val password: String
)

data class ProvisioningResponseDto(
    val ok: Boolean = false,
    val message: String? = null
)
