package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Query("SELECT * FROM trips ORDER BY timestamp DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY timestamp DESC LIMIT 10")
    fun getRecentTrips(): Flow<List<TripEntity>>

    @Query("SELECT SUM(distanceKm) FROM trips")
    fun getTotalRecordedDistance(): Flow<Float?>

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: Long)

    @Query("DELETE FROM trips")
    suspend fun clearAllTrips()
}
