package com.example.model

enum class RideMode(val label: String, val defaultMaxSpeed: Int, val description: String) {
    WALK("Piéton (Walk)", 6, "Assistance à la marche 6 km/h"),
    ECO("Éco (15 km/h)", 15, "Autonomie maximale, accélérations douces"),
    DRIVE("Standard (20 km/h)", 20, "Conforme norme allemande / urbaine"),
    SPORT("Sport (25 km/h)", 25, "Pleine puissance légale européenne"),
    BOOST("Boost (>25 km/h)", 35, "Débridage matériel (Terrain privé uniquement)")
}

enum class RegenStrength(val label: String, val level: Int) {
    LOW("Léger", 1),
    MEDIUM("Moyen", 2),
    STRONG("Fort (Récupération max)", 3)
}

enum class ConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    SIMULATED
}

data class ScooterTelemetry(
    val speedKmh: Float = 0.0f,
    val targetSpeedLimit: Int = 25,
    val confirmedEscSpeedLimit: Int = 25,
    val isSpeedLimitClampedByEsc: Boolean = false,
    val batteryPercent: Int = 85,
    val batteryVoltage: Float = 52.1f, // Pack 48V nominal (42V - 54.6V)
    val currentAmps: Float = 0.0f,
    val powerWatts: Float = 0.0f,
    val motorTempC: Int = 34,
    val controllerTempC: Int = 36,
    val tripDistanceKm: Float = 0.0f,
    val totalOdometerKm: Float = 142.6f,
    val rideMode: RideMode = RideMode.SPORT,
    val headlightOn: Boolean = false,
    val taillightBlinking: Boolean = false,
    val isLocked: Boolean = false,
    val kickStartSpeedKmh: Int = 3, // 0 = zero-start, 3, 4, 5 km/h
    val isZeroStartAcceptedByEsc: Boolean = true,
    val cruiseControlEnabled: Boolean = false,
    val regenStrength: RegenStrength = RegenStrength.MEDIUM,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val deviceName: String = "NAVEE NT5 Max",
    val deviceMac: String = "00:00:00:00:00:00",
    val rssi: Int = -60,
    val lastPacketTimestamp: Long = 0L,
    val firmwareVersion: String = "v1.2.8-Brightway",
    val hardwareVersion: String = "NT5-MAX-ESC-V2",
    val activeErrorCode: Int = 0
) {
    val estimatedRangeRemainingKm: Float
        get() = (batteryPercent * 0.65f).coerceAtLeast(0.0f) // ~65km max range on NT5 Max
}
