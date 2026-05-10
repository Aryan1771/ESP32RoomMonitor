package com.example.roommonitor.data

class RoomStatusRepository(
    private val api: RoomStatusApi
) {
    suspend fun fetchStatus(): RoomStatusDto = api.getStatus()
}
