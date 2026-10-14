package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.ScooterViewModel
import com.example.ui.components.AppNavigationBar
import com.example.ui.controls.ControlsScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.diagnostics.DiagnosticsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.telemetry.TelemetryScreen
import com.example.ui.theme.NaveeTheme
import com.example.ui.trips.TripsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NaveeTheme {
                val viewModel: ScooterViewModel = viewModel()
                val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
                val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()
                val packetHistory by viewModel.packetHistory.collectAsStateWithLifecycle()
                val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
                val bleConnectionStatus by viewModel.bleConnectionStatus.collectAsStateWithLifecycle()
                val allTrips by viewModel.allTrips.collectAsStateWithLifecycle()

                // BLE Permission Launcher
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val allGranted = permissions.values.all { it }
                    if (allGranted && !isDemoMode) {
                        viewModel.startBleScan(onlyNavee = false)
                    }
                }

                LaunchedEffect(isDemoMode) {
                    if (!isDemoMode) {
                        val neededPermissions = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
                            }
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                        } else {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }

                        if (neededPermissions.isNotEmpty()) {
                            permissionsLauncher.launch(neededPermissions.toTypedArray())
                        }
                    }
                }

                // BackHandler
                BackHandler(enabled = viewModel.currentScreen.value != AppScreen.DASHBOARD) {
                    viewModel.selectScreen(AppScreen.DASHBOARD)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AppNavigationBar(
                            currentScreen = viewModel.currentScreen.value,
                            onSelectScreen = { viewModel.selectScreen(it) }
                        )
                    }
                ) { innerPadding ->
                    val screenModifier = Modifier.padding(innerPadding)

                    when (viewModel.currentScreen.value) {
                        AppScreen.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            telemetry = telemetry,
                            isDemoMode = isDemoMode,
                            modifier = screenModifier
                        )

                        AppScreen.CONTROLS -> ControlsScreen(
                            viewModel = viewModel,
                            telemetry = telemetry,
                            modifier = screenModifier
                        )

                        AppScreen.TELEMETRY -> TelemetryScreen(
                            telemetry = telemetry,
                            modifier = screenModifier
                        )

                        AppScreen.TRIPS -> TripsScreen(
                            viewModel = viewModel,
                            trips = allTrips,
                            modifier = screenModifier
                        )

                        AppScreen.DIAGNOSTICS -> DiagnosticsScreen(
                            viewModel = viewModel,
                            telemetry = telemetry,
                            packetHistory = packetHistory,
                            modifier = screenModifier
                        )

                        AppScreen.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            isDemoMode = isDemoMode,
                            connectionStatus = bleConnectionStatus,
                            discoveredDevices = discoveredDevices,
                            modifier = screenModifier
                        )
                    }
                }
            }
        }
    }
}
