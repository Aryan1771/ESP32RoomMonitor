package com.example.roommonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roommonitor.ui.RoomStatusScreen
import com.example.roommonitor.ui.RoomStatusViewModel
import com.example.roommonitor.ui.theme.RoomMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoomMonitorTheme {
                val viewModel: RoomStatusViewModel = viewModel()
                RoomStatusScreen(viewModel = viewModel)
            }
        }
    }
}
