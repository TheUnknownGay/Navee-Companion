package com.example.model

enum class PacketDirection {
    TX, // Phone -> Scooter
    RX  // Scooter -> Phone
}

data class BlePacket(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val direction: PacketDirection,
    val commandId: Int,
    val commandName: String,
    val rawBytes: ByteArray,
    val hexString: String = rawBytes.joinToString(" ") { "%02X".format(it) },
    val description: String,
    val isSuccess: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BlePacket
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
