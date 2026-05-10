package com.example.roommonitor.data

import retrofit2.http.GET

interface RoomStatusApi {
    @GET("status")
    suspend fun getStatus(): RoomStatusDto
}
