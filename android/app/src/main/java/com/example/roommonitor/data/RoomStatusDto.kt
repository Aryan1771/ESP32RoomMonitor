package com.example.roommonitor.data

data class RoomStatusDto(
    val statusAvailable: Boolean = false,
    val message: String? = null,
    val chipTemperatureC: Double? = null,
    val lightRaw: Int? = null,
    val lightPercent: Int? = null,
    val temperatureC: Double? = null,
    val humidity: Double? = null,
    val deviceId: String? = null,
    val sleepIntervalMinutes: Double? = null,
    val deviceSentAt: String? = null,
    val serverReceivedAt: String? = null,
    val dhtEnabled: Boolean = false
)
