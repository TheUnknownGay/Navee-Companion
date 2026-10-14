package com.example.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble.DiscoveredBleDevice
import com.example.data.ScooterRepository
import com.example.data.TripEntity
import com.example.model.BlePacket
import com.example.model.ConnectionStatus
import com.example.model.ScooterTelemetry
import com.example.model.SpeedProfilePreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    DASHBOARD("Tableau de bord"),
    CONTROLS("Vitesse & Profils"),
    TELEMETRY("Télémétrie"),
    TRIPS("Trajets & Stats"),
    DIAGNOSTICS("Diagnostic BLE"),
    SETTINGS("Réglages & Sécurité")
}

class ScooterViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ScooterRepository(application, viewModelScope)

    val currentScreen = androidx.compose.runtime.mutableStateOf(AppScreen.DASHBOARD)

    val telemetry: StateFlow<ScooterTelemetry> = repository.activeTelemetry
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScooterTelemetry())

    val isDemoMode: StateFlow<Boolean> = repository.isDemoMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val packetHistory: StateFlow<List<BlePacket>> = repository.packetHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = repository.bleManager.discoveredDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bleConnectionStatus: StateFlow<ConnectionStatus> = repository.bleManager.connectionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionStatus.DISCONNECTED)

    val allTrips: StateFlow<List<TripEntity>> = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trip recording states
    var isTripRecording = androidx.compose.runtime.mutableStateOf(false)
    private var tripStartTime: Long = 0L
    private var startOdo: Float = 0.0f
    private var startBatt: Int = 100
    private var maxRecordedSpeed: Float = 0.0f

    fun selectScreen(screen: AppScreen) {
        currentScreen.value = screen
    }

    fun setDemoMode(enabled: Boolean) {
        repository.setDemoMode(enabled)
    }

    fun applySpeedPreset(preset: SpeedProfilePreset) {
        repository.applySpeedProfile(preset)
    }

    fun setCustomSpeedLimit(speedKmh: Int) {
        repository.setCustomSpeedLimit(speedKmh)
    }

    fun setKickStartSpeed(speedKmh: Int) {
        repository.setKickStartSpeed(speedKmh)
    }

    fun toggleLock() {
        repository.toggleLock()
    }

    fun toggleHeadlight() {
        repository.toggleHeadlight()
    }

    fun toggleCruiseControl() {
        repository.toggleCruiseControl()
    }

    fun simulateThrottle(pressed: Boolean) {
        repository.simulateThrottle(pressed)
        if (pressed && isTripRecording.value) {
            val speed = telemetry.value.speedKmh
            if (speed > maxRecordedSpeed) {
                maxRecordedSpeed = speed
            }
        }
    }

    fun simulateBrake(pressed: Boolean) {
        repository.simulateBrake(pressed)
    }

    fun startBleScan(onlyNavee: Boolean = false) {
        repository.startBleScan(onlyNavee)
    }

    fun stopBleScan() {
        repository.stopBleScan()
    }

    fun connectToDevice(device: BluetoothDevice) {
        repository.connectToBleDevice(device)
    }

    fun disconnectBle() {
        repository.disconnectBle()
    }

    fun clearPacketLogs() {
        repository.clearPacketHistory()
    }

    fun startTripRecording() {
        isTripRecording.value = true
        tripStartTime = System.currentTimeMillis()
        startOdo = telemetry.value.totalOdometerKm
        startBatt = telemetry.value.batteryPercent
        maxRecordedSpeed = telemetry.value.speedKmh
    }

    fun stopTripRecording() {
        if (!isTripRecording.value) return
        val durationSec = ((System.currentTimeMillis() - tripStartTime) / 1000).coerceAtLeast(1)
        val distance = (telemetry.value.totalOdometerKm - startOdo).coerceAtLeast(0.0f)
        val avgSpeed = if (durationSec > 0) (distance / (durationSec / 3600.0f)).coerceAtLeast(0.0f) else 0.0f
        val endBatt = telemetry.value.batteryPercent
        val energyWh = distance * 18.5f // ~18.5 Wh/km consommation moyenne NT5 Max

        viewModelScope.launch {
            repository.recordTrip(
                durationSec = durationSec,
                distanceKm = distance,
                maxSpeed = maxRecordedSpeed,
                avgSpeed = avgSpeed,
                energyWh = energyWh,
                startBatt = startBatt,
                endBatt = endBatt,
                mode = telemetry.value.rideMode
            )
            isTripRecording.value = false
        }
    }

    fun clearAllTrips() {
        viewModelScope.launch {
            repository.clearAllTrips()
        }
    }
}
