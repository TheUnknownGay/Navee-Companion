package com.example.ble

import com.example.model.BlePacket
import com.example.model.PacketDirection
import com.example.model.RideMode
import com.example.model.ScooterTelemetry
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Implémentation du protocole Bluetooth Low Energy pour trottinettes NAVEE / Brightway.
 * Basé sur les recherches communautaires (pepperonas, scooterteam, Brightway UART).
 */
object NaveeBleProtocol {

    // UUIDs Services et Caractéristiques
    val BRIGHTWAY_SERVICE_UUID: UUID = UUID.fromString("00001530-1212-efde-1523-785feabcd123")
    val BRIGHTWAY_TX_CHAR_UUID: UUID = UUID.fromString("0000155f-1212-efde-1523-785feabcd123")
    val BRIGHTWAY_RX_CHAR_UUID: UUID = UUID.fromString("0000155e-1212-efde-1523-785feabcd123")
    val BRIGHTWAY_AUTH_READ_UUID: UUID = UUID.fromString("00002556-1212-efde-1523-785feabcd123")
    val BRIGHTWAY_AUTH_WRITE_UUID: UUID = UUID.fromString("00002557-1212-efde-1523-785feabcd123")

    // UUIDs Nordic UART (NUS standard - fallback universel présent sur de nombreux firmwares)
    val NORDIC_UART_SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val NORDIC_UART_TX_CHAR_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    val NORDIC_UART_RX_CHAR_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

    const val FRAME_HEADER: Byte = 0x5A.toByte()

    // Commandes connues et documentées
    const val CMD_QUERY_TELEMETRY: Byte = 0x12.toByte()
    const val CMD_TELEMETRY_RESPONSE: Byte = 0x21.toByte()
    const val CMD_QUERY_VERSION: Byte = 0x14.toByte()
    const val CMD_VERSION_RESPONSE: Byte = 0x41.toByte()

    const val CMD_SET_SPEED_LIMIT: Byte = 0x6E.toByte() // Documenté pepperonas pour ST3 / Brightway
    const val CMD_SET_LOCK: Byte = 0x64.toByte()
    const val CMD_SET_LIGHTS: Byte = 0x65.toByte()
    const val CMD_SET_KICK_START: Byte = 0x66.toByte() // 0x00 pour 0 km/h, 0x03, 0x04, 0x05
    const val CMD_SET_CRUISE: Byte = 0x67.toByte()
    const val CMD_SET_REGEN: Byte = 0x68.toByte()

    /**
     * Calcule le checksum CRC16-CCITT (Poly 0x1021, Init 0x0000)
     */
    fun calculateCrc16(data: ByteArray, offset: Int, length: Int): Int {
        var crc = 0x0000
        for (i in offset until (offset + length)) {
            val b = data[i].toInt() and 0xFF
            crc = crc xor (b shl 8)
            for (j in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
                crc = crc and 0xFFFF
            }
        }
        return crc
    }

    /**
     * Construit une trame Brightway / Navee standard :
     * [0x5A] [Length] [Command] [Payload...] [CRC_L] [CRC_H]
     */
    fun buildPacket(command: Byte, payload: ByteArray = byteArrayOf()): ByteArray {
        val totalLength = 1 + 1 + 1 + payload.size + 2 // Header + Len + Cmd + Payload + CRC16
        val buffer = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put(FRAME_HEADER)
        buffer.put((payload.size + 1).toByte()) // Command + payload length
        buffer.put(command)
        if (payload.isNotEmpty()) {
            buffer.put(payload)
        }

        val array = buffer.array()
        // Calcule le CRC sur [Len, Cmd, Payload]
        val crc = calculateCrc16(array, 1, 2 + payload.size)
        buffer.putShort(crc.toShort())

        return buffer.array()
    }

    /**
     * Commande pour changer la limite de vitesse (CMD 0x6E)
     * @param speedKmh Vitesse cible (ex: 15, 20, 25, ou boost 35)
     */
    fun buildSetSpeedLimitPacket(speedKmh: Int): BlePacket {
        val payload = byteArrayOf(speedKmh.coerceIn(5, 60).toByte())
        val bytes = buildPacket(CMD_SET_SPEED_LIMIT, payload)
        return BlePacket(
            direction = PacketDirection.TX,
            commandId = CMD_SET_SPEED_LIMIT.toInt() and 0xFF,
            commandName = "CMD_SET_SPEED_LIMIT (0x6E)",
            rawBytes = bytes,
            description = "Consigne limite de vitesse : $speedKmh km/h"
        )
    }

    /**
     * Commande pour changer le seuil de démarrage (Kick-start / Zero-start)
     * @param speedKmh 0 pour démarrage direct à l'arrêt, ou 3, 4, 5 km/h
     */
    fun buildSetKickStartPacket(speedKmh: Int): BlePacket {
        val payload = byteArrayOf(speedKmh.coerceIn(0, 5).toByte())
        val bytes = buildPacket(CMD_SET_KICK_START, payload)
        return BlePacket(
            direction = PacketDirection.TX,
            commandId = CMD_SET_KICK_START.toInt() and 0xFF,
            commandName = "CMD_SET_KICK_START (0x66)",
            rawBytes = bytes,
            description = if (speedKmh == 0) "Démarrage à 0 km/h (Zero-Start)" else "Démarrage avec poussée ($speedKmh km/h)"
        )
    }

    /**
     * Commande pour verrouiller ou déverrouiller la trottinette
     */
    fun buildSetLockPacket(locked: Boolean): BlePacket {
        val payload = byteArrayOf(if (locked) 0x01.toByte() else 0x00.toByte())
        val bytes = buildPacket(CMD_SET_LOCK, payload)
        return BlePacket(
            direction = PacketDirection.TX,
            commandId = CMD_SET_LOCK.toInt() and 0xFF,
            commandName = "CMD_SET_LOCK (0x64)",
            rawBytes = bytes,
            description = if (locked) "Verrouillage électronique (Moteur résistant)" else "Déverrouillage électronique"
        )
    }

    /**
     * Commande pour allumer ou éteindre le phare avant / feux
     */
    fun buildSetLightsPacket(lightOn: Boolean): BlePacket {
        val payload = byteArrayOf(if (lightOn) 0x01.toByte() else 0x00.toByte())
        val bytes = buildPacket(CMD_SET_LIGHTS, payload)
        return BlePacket(
            direction = PacketDirection.TX,
            commandId = CMD_SET_LIGHTS.toInt() and 0xFF,
            commandName = "CMD_SET_LIGHTS (0x65)",
            rawBytes = bytes,
            description = if (lightOn) "Allumage des phares LED" else "Extinction des phares"
        )
    }

    /**
     * Commande de requête de télémétrie (Polling d'état)
     */
    fun buildQueryTelemetryPacket(): BlePacket {
        val bytes = buildPacket(CMD_QUERY_TELEMETRY, byteArrayOf())
        return BlePacket(
            direction = PacketDirection.TX,
            commandId = CMD_QUERY_TELEMETRY.toInt() and 0xFF,
            commandName = "CMD_QUERY_TELEMETRY (0x12)",
            rawBytes = bytes,
            description = "Interrogation périodique télémétrie ESC"
        )
    }

    /**
     * Décode une trame reçue de la trottinette
     */
    fun parseResponsePacket(data: ByteArray, current: ScooterTelemetry): Pair<ScooterTelemetry, BlePacket>? {
        if (data.size < 4 || data[0] != FRAME_HEADER) {
            return null
        }

        val len = data[1].toInt() and 0xFF
        if (data.size < len + 2) {
            // Trame incomplète
            return null
        }

        val cmd = data[2]
        val payload = data.copyOfRange(3, 3 + (len - 1).coerceAtLeast(0))

        val packet = BlePacket(
            direction = PacketDirection.RX,
            commandId = cmd.toInt() and 0xFF,
            commandName = when (cmd) {
                CMD_TELEMETRY_RESPONSE -> "CMD_TELEMETRY_RESP (0x21)"
                CMD_VERSION_RESPONSE -> "CMD_VERSION_RESP (0x41)"
                CMD_SET_SPEED_LIMIT -> "ACK_SET_SPEED (0x6E)"
                CMD_SET_KICK_START -> "ACK_SET_KICK (0x66)"
                CMD_SET_LOCK -> "ACK_SET_LOCK (0x64)"
                CMD_SET_LIGHTS -> "ACK_SET_LIGHTS (0x65)"
                else -> "CMD_RESP (%02X)".format(cmd)
            },
            rawBytes = data,
            description = "Trame reçue du contrôleur ESC NT5 Max"
        )

        return when (cmd) {
            CMD_TELEMETRY_RESPONSE -> {
                if (payload.size >= 12) {
                    val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
                    val rawSpeed = buffer.short.toInt() and 0xFFFF
                    val speedKmh = rawSpeed / 10.0f
                    val batteryPercent = (buffer.get().toInt() and 0xFF).coerceIn(0, 100)
                    val rawVoltage = buffer.short.toInt() and 0xFFFF
                    val voltage = rawVoltage / 100.0f // mV -> V
                    val rawCurrent = buffer.short.toInt()
                    val currentAmps = rawCurrent / 10.0f
                    val motorTemp = buffer.get().toInt()
                    val escTemp = buffer.get().toInt()
                    val errorCode = buffer.get().toInt() and 0xFF
                    val statusFlags = buffer.get().toInt() and 0xFF

                    val isLightOn = (statusFlags and 0x01) != 0
                    val isLocked = (statusFlags and 0x02) != 0
                    val escSpeedLimit = if (payload.size >= 13) payload[12].toInt() and 0xFF else current.confirmedEscSpeedLimit
                    val kickSpeed = if (payload.size >= 14) payload[13].toInt() and 0xFF else current.kickStartSpeedKmh

                    val power = (voltage * currentAmps).coerceAtLeast(0.0f)

                    val updated = current.copy(
                        speedKmh = speedKmh,
                        batteryPercent = batteryPercent,
                        batteryVoltage = if (voltage > 30f) voltage else current.batteryVoltage,
                        currentAmps = currentAmps,
                        powerWatts = power,
                        motorTempC = motorTemp,
                        controllerTempC = escTemp,
                        headlightOn = isLightOn,
                        isLocked = isLocked,
                        confirmedEscSpeedLimit = escSpeedLimit,
                        isSpeedLimitClampedByEsc = (current.targetSpeedLimit > escSpeedLimit),
                        kickStartSpeedKmh = kickSpeed,
                        isZeroStartAcceptedByEsc = (kickSpeed == 0),
                        activeErrorCode = errorCode,
                        lastPacketTimestamp = System.currentTimeMillis()
                    )
                    Pair(updated, packet.copy(description = "Télémétrie : $speedKmh km/h, $batteryPercent%, $voltage V"))
                } else {
                    Pair(current, packet)
                }
            }

            CMD_SET_SPEED_LIMIT -> {
                // L'ESC renvoie la vitesse qu'il a réellement validée
                val confirmedSpeed = if (payload.isNotEmpty()) payload[0].toInt() and 0xFF else current.targetSpeedLimit
                val clamped = current.targetSpeedLimit > confirmedSpeed
                val updated = current.copy(
                    confirmedEscSpeedLimit = confirmedSpeed,
                    isSpeedLimitClampedByEsc = clamped
                )
                val desc = if (clamped) {
                    "ESC Bridé : Consigne ${current.targetSpeedLimit} km/h plafonnée à $confirmedSpeed km/h par le firmware."
                } else {
                    "ESC Confirmé : Limite appliquée à $confirmedSpeed km/h."
                }
                Pair(updated, packet.copy(description = desc))
            }

            CMD_SET_KICK_START -> {
                // Confirmation seuil démarrage
                val confirmedKick = if (payload.isNotEmpty()) payload[0].toInt() and 0xFF else current.kickStartSpeedKmh
                val isZeroOk = (confirmedKick == 0)
                val updated = current.copy(
                    kickStartSpeedKmh = confirmedKick,
                    isZeroStartAcceptedByEsc = isZeroOk
                )
                val desc = if (isZeroOk) {
                    "Succès : Démarrage direct à 0 km/h accepté par l'ESC."
                } else {
                    "Refus Firmware : L'ESC impose un seuil de $confirmedKick km/h (sécurité EN17128)."
                }
                Pair(updated, packet.copy(description = desc))
            }

            CMD_SET_LOCK -> {
                val locked = payload.isNotEmpty() && payload[0] == 0x01.toByte()
                Pair(current.copy(isLocked = locked), packet.copy(description = if (locked) "Trottinette verrouillée" else "Trottinette déverrouillée"))
            }

            CMD_SET_LIGHTS -> {
                val lights = payload.isNotEmpty() && payload[0] == 0x01.toByte()
                Pair(current.copy(headlightOn = lights), packet.copy(description = if (lights) "Phares allumés" else "Phares éteints"))
            }

            else -> Pair(current, packet)
        }
    }
}
