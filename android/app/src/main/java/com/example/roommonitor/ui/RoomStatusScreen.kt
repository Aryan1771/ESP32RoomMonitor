package com.example.roommonitor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roommonitor.data.RoomStatusDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomStatusScreen(viewModel: RoomStatusViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("ESP32 Room Monitor")
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null && uiState.status == null -> ErrorState(uiState.errorMessage)
                uiState.status?.statusAvailable == false -> EmptyState(
                    message = uiState.status?.message ?: "No sensor data yet",
                    lastUpdated = uiState.status?.serverReceivedAt
                )
                uiState.status != null -> ContentState(
                    status = uiState.status,
                    errorMessage = uiState.errorMessage
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text("Loading latest room status...")
    }
}

@Composable
private fun ErrorState(errorMessage: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Could not load room status",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = errorMessage ?: "Unknown error")
    }
}

@Composable
private fun EmptyState(message: String, lastUpdated: String?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Waiting for the first ESP32 reading",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            Text(text = message)
        }
        item {
            Text(
                text = "Last Updated: ${formatTimestamp(lastUpdated)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContentState(status: RoomStatusDto, errorMessage: String?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!errorMessage.isNullOrBlank()) {
            item {
                Text(
                    text = "Refresh warning: $errorMessage",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            SensorCard(
                title = "Chip Temperature",
                value = status.chipTemperatureC?.let { String.format("%.1f °C", it) } ?: "--",
                subtitle = "Internal ESP32 temperature sensor",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Memory,
                        contentDescription = null
                    )
                }
            )
        }

        item {
            SensorCard(
                title = "Light Level",
                value = status.lightPercent?.let { "$it%" } ?: "--",
                subtitle = buildString {
                    append("Raw ADC: ")
                    append(status.lightRaw ?: "--")
                },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.LightMode,
                        contentDescription = null
                    )
                }
            )
        }

        item {
            SensorCard(
                title = "Device Status",
                value = status.deviceId ?: "Unknown device",
                subtitle = "Sleeps every ${status.sleepIntervalMinutes ?: "--"} minutes",
                supportingText = "Last Updated: ${formatTimestamp(status.serverReceivedAt)}",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.DeviceThermostat,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun SensorCard(
    title: String,
    value: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    supportingText: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            icon()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!supportingText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) {
        return "Not available"
    }

    return runCatching {
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(timestamp))
    }.getOrElse {
        timestamp
    }
}
