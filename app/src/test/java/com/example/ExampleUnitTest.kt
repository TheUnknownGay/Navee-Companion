package com.example

import com.example.ble.NaveeBleProtocol
import com.example.model.ScooterErrorDirectory
import com.example.model.ScooterTelemetry
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testBuildSetSpeedLimitPacket() {
        val packet = NaveeBleProtocol.buildSetSpeedLimitPacket(25)
        assertEquals(0x6E, packet.commandId)
        assertTrue(packet.rawBytes.isNotEmpty())
        assertEquals(NaveeBleProtocol.FRAME_HEADER, packet.rawBytes[0])
        // Payload has speed limit byte
        assertEquals(25.toByte(), packet.rawBytes[3])
    }

    @Test
    fun testBuildSetKickStartZeroStartPacket() {
        val packet = NaveeBleProtocol.buildSetKickStartPacket(0)
        assertEquals(0x66, packet.commandId)
        assertEquals(0.toByte(), packet.rawBytes[3])
    }

    @Test
    fun testCrc16Checksum() {
        val data = byteArrayOf(0x02, 0x6E, 0x19) // Len, Cmd, 25 km/h
        val crc = NaveeBleProtocol.calculateCrc16(data, 0, data.size)
        assertTrue(crc != 0)
    }

    @Test
    fun testParseSpeedLimitResponse() {
        val initial = ScooterTelemetry(targetSpeedLimit = 35, confirmedEscSpeedLimit = 25)
        // Simulate ESC response clamping to 25 km/h
        val escResponse = byteArrayOf(
            NaveeBleProtocol.FRAME_HEADER,
            0x02,
            NaveeBleProtocol.CMD_SET_SPEED_LIMIT,
            25.toByte(),
            0x00, 0x00
        )
        val result = NaveeBleProtocol.parseResponsePacket(escResponse, initial)
        assertNotNull(result)
        val updated = result!!.first
        assertEquals(25, updated.confirmedEscSpeedLimit)
        assertTrue(updated.isSpeedLimitClampedByEsc)
    }

    @Test
    fun testErrorDirectory() {
        val e1 = ScooterErrorDirectory.getError(1)
        assertEquals("E1", e1.displayCode)
        assertTrue(e1.title.contains("Accélérateur"))

        val e0 = ScooterErrorDirectory.getError(0)
        assertEquals("OK", e0.displayCode)
    }
}
