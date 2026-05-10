package com.example.roommonitor.data

class ProvisioningRepository(
    private val api: ProvisioningApi
) {
    suspend fun fetchStatus(): ProvisioningStatusDto = api.getStatus()

    suspend fun scanNetworks(): ProvisioningScanDto = api.scanNetworks()

    suspend fun configureWifi(ssid: String, password: String): ProvisioningResponseDto {
        return api.configureWifi(
            ProvisioningRequestDto(
                ssid = ssid,
                password = password
            )
        )
    }
}
