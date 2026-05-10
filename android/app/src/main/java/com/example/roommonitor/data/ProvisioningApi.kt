package com.example.roommonitor.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ProvisioningApi {
    @GET("status")
    suspend fun getStatus(): ProvisioningStatusDto

    @GET("scan")
    suspend fun scanNetworks(): ProvisioningScanDto

    @POST("configure")
    suspend fun configureWifi(@Body request: ProvisioningRequestDto): ProvisioningResponseDto
}
