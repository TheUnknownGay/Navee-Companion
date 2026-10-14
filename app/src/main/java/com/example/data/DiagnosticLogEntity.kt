package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "diagnostic_logs")
data class DiagnosticLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val direction: String, // TX or RX
    val commandName: String,
    val hexData: String,
    val description: String,
    val isSuccess: Boolean
)

@Dao
interface DiagnosticLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DiagnosticLogEntity)

    @Query("SELECT * FROM diagnostic_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<DiagnosticLogEntity>>

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clearLogs()
}
