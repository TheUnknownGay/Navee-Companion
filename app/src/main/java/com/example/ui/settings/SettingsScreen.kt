package com.example.ui.settings

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ble.DiscoveredBleDevice
import com.example.model.ConnectionStatus
import com.example.ui.ScooterViewModel
import com.example.ui.theme.NaveeCyan
import com.example.ui.theme.NaveeGreen
import com.example.ui.theme.NaveeOrange
import com.example.ui.theme.NaveeRed
import com.example.ui.theme.NaveeYellow

@SuppressLint("MissingPermission")
@Composable
fun SettingsScreen(
    viewModel: ScooterViewModel,
    isDemoMode: Boolean,
    connectionStatus: ConnectionStatus,
    discoveredDevices: List<DiscoveredBleDevice>,
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
                text = "RÉGLAGES & DOCUMENTATION",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Gestion de la connectivité Bluetooth, analyse technique et conformité.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. DEMO MODE SWITCH
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDemoMode) NaveeYellow.copy(alpha = 0.2f) else NaveeCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDemoMode) Icons.Default.PlayCircle else Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = if (isDemoMode) NaveeYellow else NaveeCyan
                                )
                            }
                            Column {
                                Text(
                                    text = if (isDemoMode) "Mode Simulateur Démo (Actif)" else "Mode Bluetooth Réel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isDemoMode) "Test complet sans trottinette physique" else "Connexion directe à la NAVEE NT5 Max",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = isDemoMode,
                            onCheckedChange = { viewModel.setDemoMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NaveeYellow,
                                checkedTrackColor = NaveeYellow.copy(alpha = 0.5f),
                                uncheckedThumbColor = NaveeCyan,
                                uncheckedTrackColor = NaveeCyan.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("switch_demo_mode")
                        )
                    }
                }
            }
        }

        // 2. REAL BLUETOOTH SCANNER SECTION (if not in demo mode)
        if (!isDemoMode) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.Default.BluetoothSearching, contentDescription = null, tint = NaveeCyan)
                                Text(
                                    text = "Recherche Trottinettes BLE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (connectionStatus == ConnectionStatus.SCANNING) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = NaveeCyan)
                            } else {
                                Button(
                                    onClick = { viewModel.startBleScan(onlyNavee = false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NaveeCyan),
                                    modifier = Modifier.testTag("btn_scan_ble")
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scanner")
                                }
                            }
                        }

                        if (discoveredDevices.isEmpty()) {
                            Text(
                                text = "Aucun appareil détecté pour le moment.\nVérifiez que la NAVEE NT5 Max est allumée et à proximité.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                discoveredDevices.forEach { dev ->
                                    val isNavee = dev.name.contains("NAVEE", ignoreCase = true) || dev.name.contains("Brightway", ignoreCase = true)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = dev.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isNavee) NaveeCyan else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isNavee) {
                                                    Surface(shape = RoundedCornerShape(4.dp), color = NaveeCyan.copy(alpha = 0.2f)) {
                                                        Text("NAVEE", fontSize = 9.sp, color = NaveeCyan, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${dev.address} · ${dev.rssi} dBm",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.connectToDevice(dev.device) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isNavee) NaveeCyan else MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Connecter", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. TABLEAU DE FAISABILITÉ & FAITS VÉRIFIÉS
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = NaveeOrange)
                        Text(
                            text = "Analyse & Faisabilité Technique",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FeasibilityRow(
                        title = "Profils de vitesse (15, 20, 25 km/h)",
                        status = "Vérifié & Fonctionnel",
                        statusColor = NaveeGreen,
                        details = "Commande BLE 0x6E documentée par pepperonas. L'ESC valide et applique la vitesse."
                    )

                    FeasibilityRow(
                        title = "Profil Boost (>25 km/h)",
                        status = "Firmware Dépendant",
                        statusColor = NaveeOrange,
                        details = "La commande 0x6E envoie la consigne, mais l'ESC européen (CE) la plafonne à 25 km/h. Nécessite un firmware international débridé."
                    )

                    FeasibilityRow(
                        title = "Démarrage à 0 km/h (Zero-Start)",
                        status = "Conditionnel Matériel",
                        statusColor = NaveeOrange,
                        details = "Commande 0x66 payload 0x00. Accepté sur firmwares internationaux ; rejeté ou remis à 3 km/h sur firmwares CE conformes EN17128."
                    )

                    FeasibilityRow(
                        title = "Télémétrie en temps réel",
                        status = "Vérifié",
                        statusColor = NaveeGreen,
                        details = "Trame de réponse 0x21 (vitesse 0.1 km/h, tension mV, courant, températures, odomètre)."
                    )

                    FeasibilityRow(
                        title = "Éclairage & Verrouillage moteur",
                        status = "Vérifié",
                        statusColor = NaveeGreen,
                        details = "Commandes 0x65 (phare) et 0x64 (verrouillage avec résistance magnétique du moteur)."
                    )
                }
            }
        }

        // 4. RÉGLEMENTATION ET SÉCURITÉ
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
                        Icon(imageVector = Icons.Default.Gavel, contentDescription = null, tint = NaveeRed)
                        Text(
                            text = "Réglementation & Sécurité EDPm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "• En France, les engins de déplacement personnel motorisés (EDPm) sont limités par construction à 25 km/h sur la voie publique (Décret 2019-1082).\n" +
                                "• Le port du casque est vivement recommandé en agglomération et obligatoire hors agglomération sur voies autorisées.\n" +
                                "• Ne jamais désactiver les protections thermiques ou de surintensité de la batterie.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FeasibilityRow(
    title: String,
    status: String,
    statusColor: androidx.compose.ui.graphics.Color,
    details: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.2f)) {
                Text(text = status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Text(text = details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}
