package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ConnectionStatus
import com.example.model.RideMode
import com.example.model.ScooterTelemetry
import com.example.model.SpeedProfilePreset
import com.example.ui.ScooterViewModel
import com.example.ui.theme.NaveeCyan
import com.example.ui.theme.NaveeGreen
import com.example.ui.theme.NaveeOrange
import com.example.ui.theme.NaveeRed
import com.example.ui.theme.NaveeYellow

@Composable
fun DashboardScreen(
    viewModel: ScooterViewModel,
    telemetry: ScooterTelemetry,
    isDemoMode: Boolean,
    modifier: Modifier = Modifier
) {
    var visualModeTab by remember { mutableIntStateOf(0) } // 0 = 3D Interactif, 1 = Photo Studio

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. HEADER : Status Bar & Badges
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NAVEE NT5 MAX",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isDemoMode) "Mode Démonstration Actif" else "Connexion BLE Réelle",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDemoMode) NaveeYellow else NaveeCyan
                    )
                }

                // Connection badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDemoMode) NaveeYellow.copy(alpha = 0.15f) else NaveeGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDemoMode) NaveeYellow.copy(alpha = 0.6f) else NaveeGreen.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isDemoMode) NaveeYellow else NaveeGreen)
                        )
                        Text(
                            text = if (isDemoMode) "DÉMO" else "BLE CONNECTÉ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDemoMode) NaveeYellow else NaveeGreen
                        )
                    }
                }
            }
        }

        // 2. VEHICLE VISUAL REPRESENTATION (CENTERPIECE)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // View mode switcher (3D interactif vs Photo Studio)
                    TabRow(
                        selectedTabIndex = visualModeTab,
                        containerColor = Color.Transparent,
                        contentColor = NaveeCyan,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Tab(
                            selected = visualModeTab == 0,
                            onClick = { visualModeTab = 0 },
                            text = { Text("Vue 3D Interactive", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = visualModeTab == 1,
                            onClick = { visualModeTab = 1 },
                            text = { Text("Photo Studio NT5", fontSize = 12.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (visualModeTab == 0) {
                        VehicleModelCanvas(telemetry = telemetry)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_navee_nt5_hero),
                                contentDescription = "NAVEE NT5 Max Studio",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "NAVEE NT5 MAX · Moteur 1000W Crête",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. SPEEDOMETER GAUGE
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SpeedometerGauge(telemetry = telemetry)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Warning if ESC clamped the speed limit
                    AnimatedVisibility(visible = telemetry.isSpeedLimitClampedByEsc) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Bridage Firmware : Consigne ${telemetry.targetSpeedLimit} km/h limitée à ${telemetry.confirmedEscSpeedLimit} km/h par l'ESC",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Status Chips (Zero-start, Lock, Lights)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            icon = Icons.Default.Speed,
                            title = "Démarrage",
                            value = if (telemetry.kickStartSpeedKmh == 0) "0 km/h (Direct)" else "${telemetry.kickStartSpeedKmh} km/h (Poussée)",
                            color = if (telemetry.kickStartSpeedKmh == 0) NaveeGreen else NaveeCyan
                        )
                        StatusPill(
                            icon = Icons.Default.BatteryChargingFull,
                            title = "Batterie",
                            value = "${telemetry.batteryPercent}% (${"%.1f".format(telemetry.estimatedRangeRemainingKm)} km)",
                            color = if (telemetry.batteryPercent > 20) NaveeGreen else NaveeRed
                        )
                        StatusPill(
                            icon = Icons.Default.Thermostat,
                            title = "Temp. Moteur",
                            value = "${telemetry.motorTempC}°C",
                            color = if (telemetry.motorTempC < 65) NaveeCyan else NaveeRed
                        )
                    }
                }
            }
        }

        // 4. QUICK COMMANDS (Lights, Lock, Cruise)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Phare LED
                FilledTonalButton(
                    onClick = { viewModel.toggleHeadlight() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("btn_quick_lights"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (telemetry.headlightOn) NaveeCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (telemetry.headlightOn) NaveeCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (telemetry.headlightOn) Icons.Default.Lightbulb else Icons.Default.Highlight,
                        contentDescription = "Phares",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (telemetry.headlightOn) "Phares ON" else "Phares OFF",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Verrouillage
                FilledTonalButton(
                    onClick = { viewModel.toggleLock() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("btn_quick_lock"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (telemetry.isLocked) NaveeRed.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (telemetry.isLocked) NaveeRed else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (telemetry.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Verrouillage",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (telemetry.isLocked) "Verrouillé" else "Déverrouillé",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. DEMO SIMULATOR TOUCH CONTROLS (Only visible in Demo Mode)
        if (isDemoMode) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaveeYellow.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🎮 COMMANDES TACTILES SIMULATEUR",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NaveeYellow
                            )
                            Text(
                                text = "Maintenir appuyé",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Frein
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(NaveeRed.copy(alpha = 0.2f))
                                    .border(1.5.dp, NaveeRed.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                viewModel.simulateBrake(true)
                                                tryAwaitRelease()
                                                viewModel.simulateBrake(false)
                                            }
                                        )
                                    }
                                    .testTag("btn_sim_brake"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🛑 FREIN (Disque + eABS)",
                                    color = NaveeRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // Accélérateur
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(NaveeCyan.copy(alpha = 0.25f))
                                    .border(1.5.dp, NaveeCyan, RoundedCornerShape(16.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                viewModel.simulateThrottle(true)
                                                tryAwaitRelease()
                                                viewModel.simulateThrottle(false)
                                            }
                                        )
                                    }
                                    .testTag("btn_sim_throttle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚡ ACCÉLÉRATEUR",
                                    color = NaveeCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. METRICS OVERVIEW (Voltage, Odometer, Trip)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Tension Pack",
                    value = "${"%.1f".format(telemetry.batteryVoltage)} V",
                    sub = "48V nominal",
                    color = NaveeCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Trajet En Cours",
                    value = "${"%.2f".format(telemetry.tripDistanceKm)} km",
                    sub = "Session active",
                    color = NaveeGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Odomètre Total",
                    value = "${"%.1f".format(telemetry.totalOdometerKm)} km",
                    sub = "Total trottinette",
                    color = NaveeOrange,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatusPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
        }
    }
}
