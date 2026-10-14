package com.example.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TripEntity
import com.example.ui.ScooterViewModel
import com.example.ui.theme.NaveeCyan
import com.example.ui.theme.NaveeGreen
import com.example.ui.theme.NaveeOrange
import com.example.ui.theme.NaveeRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripsScreen(
    viewModel: ScooterViewModel,
    trips: List<TripEntity>,
    modifier: Modifier = Modifier
) {
    val isRecording = viewModel.isTripRecording.value
    val totalDist = trips.sumOf { it.distanceKm.toDouble() }
    val totalEnergy = trips.sumOf { it.energyWattHours.toDouble() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HISTORIQUE DES TRAJETS",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Statistiques de conduite et consommation de la NT5 Max.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (trips.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllTrips() },
                        modifier = Modifier.testTag("btn_clear_trips")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Effacer l'historique", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // 1. RECORDING CONTROLLER CARD
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRecording) NaveeRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isRecording) NaveeRed else Color.Transparent
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRecording) "Enregistrement en cours..." else "Enregistreur de Trajet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording) NaveeRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isRecording) "Mesure de la vitesse max, moyenne et énergie" else "Prêt à démarrer une session",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopTripRecording()
                            } else {
                                viewModel.startTripRecording()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) NaveeRed else NaveeCyan
                        ),
                        modifier = Modifier.testTag("btn_record_trip")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRecording) "Terminer" else "Démarrer")
                    }
                }
            }
        }

        // 2. OVERALL STATS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TripStatCard(
                    title = "Distance Totale",
                    value = "${"%.1f".format(totalDist)} km",
                    color = NaveeCyan,
                    modifier = Modifier.weight(1f)
                )
                TripStatCard(
                    title = "Énergie Totale",
                    value = "${totalEnergy.toInt()} Wh",
                    color = NaveeOrange,
                    modifier = Modifier.weight(1f)
                )
                TripStatCard(
                    title = "Trajets Enregistrés",
                    value = "${trips.size}",
                    color = NaveeGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. TRIP LIST
        item {
            Text(
                text = "Trajets Récents",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trips.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun trajet enregistré pour l'instant.\nAppuyez sur 'Démarrer' pour enregistrer une sortie.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(trips, key = { it.id }) { trip ->
                TripItemCard(trip = trip)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TripStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun TripItemCard(trip: TripEntity) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.FRENCH)
    val dateStr = dateFormat.format(Date(trip.timestamp))
    val minutes = trip.durationSeconds / 60
    val seconds = trip.durationSeconds % 60

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = trip.rideModeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = NaveeCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NaveeCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Route, contentDescription = null, tint = NaveeCyan, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "${"%.2f".format(trip.distanceKm)} km",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Durée : ${minutes}m ${seconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Vmax : ${"%.1f".format(trip.maxSpeedKmh)} km/h",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = NaveeOrange
                    )
                    Text(
                        text = "Vmoy : ${"%.1f".format(trip.avgSpeedKmh)} km/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${trip.startBatteryPercent}% ➔ ${trip.endBatteryPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaveeGreen
                    )
                }
            }
        }
    }
}
