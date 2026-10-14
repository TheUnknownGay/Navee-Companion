package com.example.ui.telemetry

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScooterTelemetry
import com.example.ui.theme.NaveeCyan
import com.example.ui.theme.NaveeGreen
import com.example.ui.theme.NaveeOrange
import com.example.ui.theme.NaveeRed

@Composable
fun TelemetryScreen(
    telemetry: ScooterTelemetry,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TÉLÉMÉTRIE EN TEMPS RÉEL",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Données brutes issues des capteurs et de l'ESC Brightway de la NAVEE NT5 Max.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. BATTERY PACK CARD
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, tint = NaveeGreen)
                            Text(
                                text = "Pack Batterie (13S Li-ion 48V)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${telemetry.batteryPercent} %",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (telemetry.batteryPercent > 20) NaveeGreen else NaveeRed
                        )
                    }

                    LinearProgressIndicator(
                        progress = { telemetry.batteryPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (telemetry.batteryPercent > 20) NaveeGreen else NaveeRed,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tension : ${"%.2f".format(telemetry.batteryVoltage)} V",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Autonomie estimée : ~${"%.1f".format(telemetry.estimatedRangeRemainingKm)} km",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NaveeCyan
                        )
                    }
                }
            }
        }

        // 2. POWER & CURRENT GRID
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TelemetryDetailCard(
                    title = "Puissance Instantanée",
                    value = "${telemetry.powerWatts.toInt()} W",
                    caption = "Crête admissible : 1000W",
                    icon = Icons.Default.ElectricBolt,
                    color = NaveeOrange,
                    modifier = Modifier.weight(1f)
                )

                TelemetryDetailCard(
                    title = "Courant Décharge",
                    value = "${"%.1f".format(telemetry.currentAmps)} A",
                    caption = if (telemetry.currentAmps < 0) "Régénération eABS" else "Consommation moteur",
                    icon = Icons.Default.Speed,
                    color = if (telemetry.currentAmps < 0) NaveeGreen else NaveeCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. TEMPERATURE SENSORS
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Thermostat, contentDescription = null, tint = NaveeRed)
                        Text(
                            text = "Sondes Thermiques",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Moteur Roue Motrice", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${telemetry.motorTempC} °C",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (telemetry.motorTempC < 65) NaveeCyan else NaveeRed
                            )
                            Text(
                                text = if (telemetry.motorTempC < 65) "Normal (< 75°C)" else "Surchauffe!",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (telemetry.motorTempC < 65) NaveeGreen else NaveeRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Contrôleur ESC (MOSFETs)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${telemetry.controllerTempC} °C",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (telemetry.controllerTempC < 65) NaveeCyan else NaveeRed
                            )
                            Text(
                                text = if (telemetry.controllerTempC < 65) "Normal (< 80°C)" else "Surchauffe!",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (telemetry.controllerTempC < 65) NaveeGreen else NaveeRed
                            )
                        }
                    }
                }
            }
        }

        // 4. HARDWARE & FIRMWARE INFO
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = NaveeCyan)
                        Text(
                            text = "Informations Matérielles & Firmware",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TelemetryInfoRow(label = "Modèle Détecté", value = telemetry.deviceName)
                    TelemetryInfoRow(label = "Adresse MAC Bluetooth", value = telemetry.deviceMac)
                    TelemetryInfoRow(label = "Contrôleur Hardware", value = telemetry.hardwareVersion)
                    TelemetryInfoRow(label = "Version Firmware ESC", value = telemetry.firmwareVersion)
                    TelemetryInfoRow(label = "Signal RSSI BLE", value = "${telemetry.rssi} dBm")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TelemetryDetailCard(
    title: String,
    value: String,
    caption: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
            Text(text = caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp)
        }
    }
}

@Composable
fun TelemetryInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
