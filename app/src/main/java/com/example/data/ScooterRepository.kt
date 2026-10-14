package com.example.data

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.example.ble.DiscoveredBleDevice
import com.example.ble.NaveeBleManager
import com.example.ble.NaveeSimulator
import com.example.model.BlePacket
import com.example.model.ConnectionStatus
import com.example.model.RideMode
import com.example.model.ScooterTelemetry
import com.example.model.SpeedProfilePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScooterRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val database = NaveeDatabase.getDatabase(context)
    private val tripDao = database.tripDao()
    private val logDao = database.diagnosticLogDao()

    val bleManager = NaveeBleManager(context, scope)
    val simulator = NaveeSimulator(scope)

    private val _isDemoMode = MutableStateFlow(true) // Start in demo mode so user sees instant working UI without pairing
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _activeTelemetry = MutableStateFlow(ScooterTelemetry())
    val activeTelemetry: StateFlow<ScooterTelemetry> = _activeTelemetry.asStateFlow()

    private val _packetHistory = MutableStateFlow<List<BlePacket>>(emptyList())
    val packetHistory: StateFlow<List<BlePacket>> = _packetHistory.asStateFlow()

    val allTrips: Flow<List<TripEntity>> = tripDao.getAllTrips()

    init {
        // Collect simulator telemetry
        scope.launch {
            simulator.telemetry.collect { simTelem ->
                if (_isDemoMode.value) {
                    _activeTelemetry.value = simTelem
                }
            }
        }

        // Collect real BLE telemetry
        scope.launch {
            bleManager.telemetry.collect { bleTelem ->
                if (!_isDemoMode.value) {
                    _activeTelemetry.value = bleTelem
                }
            }
        }

        // Collect simulator packets
        scope.launch {
            simulator.packetLog.collect { packet ->
                if (_isDemoMode.value) {
                    addPacketToHistory(packet)
                }
            }
        }

        // Collect real BLE packets
        scope.launch {
            bleManager.packetLog.collect { packet ->
                if (!_isDemoMode.value) {
                    addPacketToHistory(packet)
                }
            }
        }

        // Seed initial rich sample trips if empty
        scope.launch(Dispatchers.IO) {
            seedSampleTripsIfNeeded()
        }

        // Start simulator by default
        simulator.startSimulation()
    }

    private fun addPacketToHistory(packet: BlePacket) {
        val list = _packetHistory.value.toMutableList()
        list.add(0, packet)
        if (list.size > 200) {
            list.removeAt(list.size - 1)
        }
        _packetHistory.value = list

        scope.launch(Dispatchers.IO) {
            try {
                logDao.insertLog(
                    DiagnosticLogEntity(
                        timestamp = packet.timestamp,
                        direction = packet.direction.name,
                        commandName = packet.commandName,
                        hexData = packet.hexString,
                        description = packet.description,
                        isSuccess = packet.isSuccess
                    )
                )
            } catch (_: Exception) {}
        }
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        if (enabled) {
            bleManager.disconnect()
            simulator.startSimulation()
            _activeTelemetry.value = simulator.telemetry.value
        } else {
            simulator.stopSimulation()
            _activeTelemetry.value = bleManager.telemetry.value
            bleManager.startScan(onlyNavee = false)
        }
    }

    fun applySpeedProfile(preset: SpeedProfilePreset) {
        if (_isDemoMode.value) {
            simulator.applySpeedProfile(preset.speedLimitKmh, preset.isBoost)
        } else {
            bleManager.sendSpeedLimit(preset.speedLimitKmh)
        }
    }

    fun setCustomSpeedLimit(speedKmh: Int) {
        val isBoost = speedKmh > 25
        if (_isDemoMode.value) {
            simulator.applySpeedProfile(speedKmh, isBoost)
        } else {
            bleManager.sendSpeedLimit(speedKmh)
        }
    }

    fun setKickStartSpeed(speedKmh: Int) {
        if (_isDemoMode.value) {
            simulator.setKickStartSpeed(speedKmh)
        } else {
            bleManager.sendKickStart(speedKmh)
        }
    }

    fun toggleLock() {
        if (_isDemoMode.value) {
            simulator.toggleLock()
        } else {
            val currentLock = _activeTelemetry.value.isLocked
            bleManager.sendLock(!currentLock)
        }
    }

    fun toggleHeadlight() {
        if (_isDemoMode.value) {
            simulator.toggleHeadlight()
        } else {
            val currentLight = _activeTelemetry.value.headlightOn
            bleManager.sendLights(!currentLight)
        }
    }

    fun toggleCruiseControl() {
        if (_isDemoMode.value) {
            simulator.toggleCruiseControl()
        }
    }

    fun simulateThrottle(pressed: Boolean) {
        if (_isDemoMode.value) {
            simulator.setThrottle(pressed)
        }
    }

    fun simulateBrake(pressed: Boolean) {
        if (_isDemoMode.value) {
            simulator.setBrake(pressed)
        }
    }

    fun startBleScan(onlyNavee: Boolean = false) {
        bleManager.startScan(onlyNavee)
    }

    fun stopBleScan() {
        bleManager.stopScan()
    }

    fun connectToBleDevice(device: BluetoothDevice) {
        _isDemoMode.value = false
        simulator.stopSimulation()
        bleManager.connect(device)
    }

    fun disconnectBle() {
        bleManager.disconnect()
    }

    fun clearPacketHistory() {
        _packetHistory.value = emptyList()
        scope.launch(Dispatchers.IO) {
            logDao.clearLogs()
        }
    }

    suspend fun recordTrip(
        durationSec: Long,
        distanceKm: Float,
        maxSpeed: Float,
        avgSpeed: Float,
        energyWh: Float,
        startBatt: Int,
        endBatt: Int,
        mode: RideMode
    ) {
        tripDao.insertTrip(
            TripEntity(
                durationSeconds = durationSec,
                distanceKm = distanceKm,
                maxSpeedKmh = maxSpeed,
                avgSpeedKmh = avgSpeed,
                energyWattHours = energyWh,
                startBatteryPercent = startBatt,
                endBatteryPercent = endBatt,
                rideModeName = mode.label
            )
        )
    }

    suspend fun clearAllTrips() {
        tripDao.clearAllTrips()
    }

    private suspend fun seedSampleTripsIfNeeded() {
        // Ajout d'exemples réalistes de trajets NT5 Max
        tripDao.insertTrip(
            TripEntity(
                timestamp = System.currentTimeMillis() - 86400000L * 2,
                durationSeconds = 1420,
                distanceKm = 8.4f,
                maxSpeedKmh = 25.0f,
                avgSpeedKmh = 21.3f,
                energyWattHours = 125.4f,
                startBatteryPercent = 95,
                endBatteryPercent = 82,
                rideModeName = "Sport (25 km/h)"
            )
        )
        tripDao.insertTrip(
            TripEntity(
                timestamp = System.currentTimeMillis() - 86400000L,
                durationSeconds = 2150,
                distanceKm = 14.1f,
                maxSpeedKmh = 25.0f,
                avgSpeedKmh = 19.8f,
                energyWattHours = 210.0f,
                startBatteryPercent = 100,
                endBatteryPercent = 78,
                rideModeName = "Standard (20 km/h)"
            )
        )
        tripDao.insertTrip(
            TripEntity(
                timestamp = System.currentTimeMillis() - 3600000L * 4,
                durationSeconds = 860,
                distanceKm = 5.2f,
                maxSpeedKmh = 24.5f,
                avgSpeedKmh = 22.0f,
                energyWattHours = 82.0f,
                startBatteryPercent = 78,
                endBatteryPercent = 69,
                rideModeName = "Sport (25 km/h)"
            )
        )
    }
}
