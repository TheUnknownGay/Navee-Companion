package com.example.ble

import com.example.model.BlePacket
import com.example.model.ConnectionStatus
import com.example.model.PacketDirection
import com.example.model.RideMode
import com.example.model.ScooterTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Simulateur officiel haute fidélité pour NAVEE NT5 Max.
 * Permet de tester toute l'interface, les animations 3D et les commandes sans trottinette physique.
 */
class NaveeSimulator(private val scope: CoroutineScope) {

    private val _telemetry = MutableStateFlow(
        ScooterTelemetry(
            connectionStatus = ConnectionStatus.SIMULATED,
            deviceName = "NAVEE NT5 Max (Simulateur)",
            deviceMac = "AA:BB:CC:DD:EE:FF",
            rssi = -42,
            targetSpeedLimit = 25,
            confirmedEscSpeedLimit = 25,
            kickStartSpeedKmh = 3,
            isZeroStartAcceptedByEsc = true,
            batteryPercent = 88,
            batteryVoltage = 52.6f,
            totalOdometerKm = 348.5f,
            firmwareVersion = "v1.2.8-Brightway-NT5"
        )
    )
    val telemetry: StateFlow<ScooterTelemetry> = _telemetry.asStateFlow()

    private val _packetLog = MutableSharedFlow<BlePacket>(extraBufferCapacity = 64)
    val packetLog: SharedFlow<BlePacket> = _packetLog.asSharedFlow()

    private var simulationJob: Job? = null
    private var isThrottlePressed: Boolean = false
    private var isBrakePressed: Boolean = false

    fun startSimulation() {
        if (simulationJob?.isActive == true) return

        simulationJob = scope.launch(Dispatchers.Default) {
            _telemetry.value = _telemetry.value.copy(
                connectionStatus = ConnectionStatus.SIMULATED
            )
            emitLog(
                BlePacket(
                    direction = PacketDirection.RX,
                    commandId = 0x21,
                    commandName = "SIM_INIT",
                    rawBytes = byteArrayOf(0x5A, 0x05, 0x21, 0x00, 0x00),
                    description = "Connexion au simulateur NAVEE NT5 Max démarrée"
                )
            )

            var lastTickTime = System.currentTimeMillis()

            while (isActive) {
                delay(100) // 10 Hz refresh
                val now = System.currentTimeMillis()
                val dt = (now - lastTickTime) / 1000.0f
                lastTickTime = now

                updatePhysics(dt)
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        _telemetry.value = _telemetry.value.copy(
            connectionStatus = ConnectionStatus.DISCONNECTED,
            speedKmh = 0.0f,
            powerWatts = 0.0f
        )
    }

    private fun updatePhysics(dt: Float) {
        val current = _telemetry.value

        if (current.isLocked) {
            // Moteur verrouillé = vitesse nulle
            _telemetry.value = current.copy(
                speedKmh = 0.0f,
                powerWatts = 0.0f,
                currentAmps = 0.0f
            )
            return
        }

        var speed = current.speedKmh
        val targetLimit = current.confirmedEscSpeedLimit.toFloat()

        if (isThrottlePressed) {
            // Accélération franche (moteur 500W nominal / 1000W crête sur NT5 Max)
            val accel = when (current.rideMode) {
                RideMode.WALK -> 1.5f
                RideMode.ECO -> 3.0f
                RideMode.DRIVE -> 4.5f
                RideMode.SPORT, RideMode.BOOST -> 6.5f
            }
            speed = (speed + accel * dt).coerceAtMost(targetLimit)
        } else if (isBrakePressed) {
            // Freinage puissant à disque + eABS régénératif
            val decel = 9.0f
            speed = (speed - decel * dt).coerceAtLeast(0.0f)
        } else {
            // Décélération naturelle (frottement roulement + air)
            val coastDecel = 1.2f
            speed = (speed - coastDecel * dt).coerceAtLeast(0.0f)
        }

        // Calcul puissance électrique instantanée
        val amps = if (speed > 0.5f && isThrottlePressed) {
            (speed / targetLimit) * 18.0f + Random.nextFloat() * 1.5f
        } else if (speed > 0.5f && isBrakePressed) {
            // Régénération négative
            -4.5f
        } else {
            0.2f
        }

        val voltage = (54.6f - (100 - current.batteryPercent) * 0.12f).coerceIn(42.0f, 54.6f)
        val power = (voltage * amps).coerceAtLeast(0.0f)

        // Incrément distance parcourue
        val distInc = (speed * (dt / 3600.0f))
        val newTrip = current.tripDistanceKm + distInc
        val newOdo = current.totalOdometerKm + distInc

        // Légère variation température
        val targetTemp = if (speed > 25.0f) 46 else 34
        val motorTemp = if (current.motorTempC < targetTemp) current.motorTempC + 1 else current.motorTempC

        _telemetry.value = current.copy(
            speedKmh = speed,
            currentAmps = amps,
            powerWatts = power,
            batteryVoltage = voltage,
            motorTempC = motorTemp,
            tripDistanceKm = newTrip,
            totalOdometerKm = newOdo,
            lastPacketTimestamp = System.currentTimeMillis()
        )
    }

    fun setThrottle(pressed: Boolean) {
        isThrottlePressed = pressed
        if (pressed) isBrakePressed = false
    }

    fun setBrake(pressed: Boolean) {
        isBrakePressed = pressed
        if (pressed) isThrottlePressed = false
    }

    fun applySpeedProfile(speedLimitKmh: Int, isBoost: Boolean) {
        val current = _telemetry.value
        // Simulation firmware : en Europe, si le firmware d'origine est verrouillé, il bride à 25 km/h
        val confirmed = if (isBoost) {
            35 // En mode Boost dans le simulateur, on autorise la démonstration 35 km/h
        } else {
            speedLimitKmh
        }

        val mode = when {
            speedLimitKmh <= 6 -> RideMode.WALK
            speedLimitKmh <= 15 -> RideMode.ECO
            speedLimitKmh <= 20 -> RideMode.DRIVE
            speedLimitKmh <= 25 -> RideMode.SPORT
            else -> RideMode.BOOST
        }

        _telemetry.value = current.copy(
            targetSpeedLimit = speedLimitKmh,
            confirmedEscSpeedLimit = confirmed,
            isSpeedLimitClampedByEsc = (speedLimitKmh > confirmed),
            rideMode = mode
        )

        val txPacket = NaveeBleProtocol.buildSetSpeedLimitPacket(speedLimitKmh)
        emitLog(txPacket)

        val rxPacket = BlePacket(
            direction = PacketDirection.RX,
            commandId = 0x6E,
            commandName = "ACK_SET_SPEED (0x6E)",
            rawBytes = byteArrayOf(0x5A, 0x02, 0x6E, confirmed.toByte(), 0x00),
            description = "ESC NT5 Max Simulé : Limite fixée à $confirmed km/h"
        )
        emitLog(rxPacket)
    }

    fun setKickStartSpeed(speedKmh: Int) {
        val current = _telemetry.value
        // Dans le simulateur, le 0 km/h est accepté et démontre la réponse d'un ESC réceptif
        _telemetry.value = current.copy(
            kickStartSpeedKmh = speedKmh,
            isZeroStartAcceptedByEsc = (speedKmh == 0)
        )

        val txPacket = NaveeBleProtocol.buildSetKickStartPacket(speedKmh)
        emitLog(txPacket)

        val rxPacket = BlePacket(
            direction = PacketDirection.RX,
            commandId = 0x66,
            commandName = "ACK_SET_KICK (0x66)",
            rawBytes = byteArrayOf(0x5A, 0x02, 0x66, speedKmh.toByte(), 0x00),
            description = if (speedKmh == 0) "Zero-Start activé (Démarrage sans poussée)" else "Seuil poussée : $speedKmh km/h"
        )
        emitLog(rxPacket)
    }

    fun toggleHeadlight() {
        val current = _telemetry.value
        val newState = !current.headlightOn
        _telemetry.value = current.copy(headlightOn = newState)

        val tx = NaveeBleProtocol.buildSetLightsPacket(newState)
        emitLog(tx)
    }

    fun toggleLock() {
        val current = _telemetry.value
        val newState = !current.isLocked
        _telemetry.value = current.copy(
            isLocked = newState,
            speedKmh = if (newState) 0.0f else current.speedKmh
        )

        val tx = NaveeBleProtocol.buildSetLockPacket(newState)
        emitLog(tx)
    }

    fun toggleCruiseControl() {
        val current = _telemetry.value
        _telemetry.value = current.copy(cruiseControlEnabled = !current.cruiseControlEnabled)
    }

    fun resetTrip() {
        _telemetry.value = _telemetry.value.copy(tripDistanceKm = 0.0f)
    }

    private fun emitLog(packet: BlePacket) {
        scope.launch {
            _packetLog.emit(packet)
        }
    }
}
