package com.example.ui.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RegenStrength
import com.example.model.ScooterTelemetry
import com.example.model.SpeedProfilePreset
import com.example.ui.ScooterViewModel
import com.example.ui.theme.NaveeCyan
import com.example.ui.theme.NaveeGreen
import com.example.ui.theme.NaveeOrange
import com.example.ui.theme.NaveeRed
import com.example.ui.theme.NaveeYellow

@Composable
fun ControlsScreen(
    viewModel: ScooterViewModel,
    telemetry: ScooterTelemetry,
    modifier: Modifier = Modifier
) {
    var showBoostWarningDialog by remember { mutableStateOf(false) }
    var sliderValue by remember(telemetry.targetSpeedLimit) {
        mutableFloatStateOf(telemetry.targetSpeedLimit.toFloat())
    }

    if (showBoostWarningDialog) {
        AlertDialog(
            onDismissRequest = { showBoostWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = NaveeOrange,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Avertissement Mode Boost (>25 km/h)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "En France et dans l'Union Européenne, la vitesse maximale autorisée sur la voie publique pour les EDPm est de 25 km/h (Décret n°2019-1082).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Ce profil est strictement réservé aux terrains privés.\n" +
                                "• Sur les firmwares NAVEE NT5 Max européens d'origine (norme CE), le contrôleur ESC bride matériellement la vitesse à 25 km/h même si l'application transmet la commande 0x6E.\n" +
                                "• L'application vous indiquera si l'ESC a réellement accepté ou ignoré cette consigne.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBoostWarningDialog = false
                        viewModel.applySpeedPreset(SpeedProfilePreset.PROFILE_BOOST)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaveeOrange)
                ) {
                    Text("J'accepte les conditions")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBoostWarningDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PROFILS DE VITESSE",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Sélectionnez un profil préconfiguré ou ajustez la consigne au km/h près.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. SPEED PROFILES PRESET CARDS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SpeedProfilePreset.values().forEach { preset ->
                    val isSelected = telemetry.targetSpeedLimit == preset.speedLimitKmh

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (preset.isBoost) {
                                    showBoostWarningDialog = true
                                } else {
                                    viewModel.applySpeedPreset(preset)
                                }
                            }
                            .testTag("preset_card_${preset.speedLimitKmh}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                if (preset.isBoost) NaveeOrange.copy(alpha = 0.15f) else NaveeCyan.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(
                                1.5.dp,
                                if (preset.isBoost) NaveeOrange else NaveeCyan
                            )
                        } else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (preset.isBoost) NaveeOrange.copy(alpha = 0.2f) else NaveeCyan.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (preset.isBoost) Icons.Default.FlashOn else Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = if (preset.isBoost) NaveeOrange else NaveeCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "${preset.title} · ${preset.subtitle}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = preset.legalStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Sélectionné",
                                    tint = if (preset.isBoost) NaveeOrange else NaveeCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. FINE TUNING SLIDER (pepperonas CMD 0x6E feature)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = NaveeCyan)
                            Text(
                                text = "Réglage Fin de Vitesse",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${sliderValue.toInt()} km/h",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (sliderValue > 25) NaveeOrange else NaveeCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            viewModel.setCustomSpeedLimit(sliderValue.toInt())
                        },
                        valueRange = 10f..35f,
                        steps = 24, // 1 km/h step
                        colors = SliderDefaults.colors(
                            thumbColor = if (sliderValue > 25) NaveeOrange else NaveeCyan,
                            activeTrackColor = if (sliderValue > 25) NaveeOrange else NaveeCyan
                        ),
                        modifier = Modifier.testTag("speed_slider")
                    )

                    // ESC Hardware feedback banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Retour ESC Contrôleur :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${telemetry.confirmedEscSpeedLimit} km/h (Confirmé)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (telemetry.isSpeedLimitClampedByEsc) NaveeRed else NaveeGreen
                        )
                    }
                }
            }
        }

        // 3. ZERO-START (DÉMARRAGE À 0 KM/H)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ElectricScooter, contentDescription = null, tint = NaveeGreen)
                        Text(
                            text = "Seuil de Démarrage (Zero-Start)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "L'application officielle NAVEE limite les options à 3, 4 et 5 km/h conformément à la norme européenne EN 17128. NAVEE Companion vous permet d'envoyer la commande de démarrage direct à 0 km/h.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Threshold selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 3, 4, 5).forEach { speed ->
                            val isSelected = telemetry.kickStartSpeedKmh == speed
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setKickStartSpeed(speed) },
                                label = {
                                    Text(
                                        text = if (speed == 0) "0 km/h (Direct)" else "$speed km/h",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (speed == 0) NaveeGreen.copy(alpha = 0.2f) else NaveeCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = if (speed == 0) NaveeGreen else NaveeCyan
                                ),
                                modifier = Modifier.weight(1f).testTag("chip_kick_$speed")
                            )
                        }
                    }

                    // Live ESC Feedback status
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (telemetry.isZeroStartAcceptedByEsc) NaveeGreen.copy(alpha = 0.1f) else NaveeOrange.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, if (telemetry.isZeroStartAcceptedByEsc) NaveeGreen.copy(alpha = 0.4f) else NaveeOrange.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (telemetry.isZeroStartAcceptedByEsc) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (telemetry.isZeroStartAcceptedByEsc) NaveeGreen else NaveeOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (telemetry.isZeroStartAcceptedByEsc) {
                                        "Validation ESC : Démarrage direct à 0 km/h actif"
                                    } else {
                                        "Seuil Contrôleur Actif : ${telemetry.kickStartSpeedKmh} km/h"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (telemetry.isZeroStartAcceptedByEsc) NaveeGreen else NaveeOrange
                                )
                                Text(
                                    text = if (telemetry.isZeroStartAcceptedByEsc) {
                                        "L'accélérateur réagit dès l'arrêt complet sans impulsion du pied."
                                    } else {
                                        "Si votre trottinette ne réagit pas à 0 km/h, le firmware ESC Brightway verrouille le seuil minimal de poussée à 3 km/h."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. CRUISE CONTROL & REGENERATIVE BRAKING
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "FONCTIONS COMPLÉMENTAIRES",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Cruise control switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Régulateur de Vitesse (Cruise Control)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Maintient l'allure après 5 secondes à vitesse constante",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = telemetry.cruiseControlEnabled,
                            onCheckedChange = { viewModel.toggleCruiseControl() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NaveeCyan,
                                checkedTrackColor = NaveeCyan.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("switch_cruise")
                        )
                    }

                    // Regen strength
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Freinage Régénératif (eABS & KERS)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RegenStrength.values().forEach { regen ->
                                val isSelected = telemetry.regenStrength == regen
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { /* Set regen */ },
                                    label = { Text(regen.label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
