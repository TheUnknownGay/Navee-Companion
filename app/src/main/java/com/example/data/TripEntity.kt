package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long,
    val distanceKm: Float,
    val maxSpeedKmh: Float,
    val avgSpeedKmh: Float,
    val energyWattHours: Float,
    val startBatteryPercent: Int,
    val endBatteryPercent: Int,
    val rideModeName: String
)
