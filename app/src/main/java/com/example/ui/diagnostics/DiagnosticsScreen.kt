package com.example.ui.diagnostics

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ble.NaveeBleProtocol
import com.example.model.BlePacket
import com.example.model.PacketDirection
import com.example.model.ScooterErrorDirectory
import com.example.model.ScooterTelemetry
import com.example.ui.ScooterViewModel
import com.example.ui.theme.NaveeCyan
import com.example.ui.theme.NaveeGreen
import com.example.ui.theme.NaveeOrange
import com.example.ui.theme.NaveeRed
import com.example.ui.theme.NaveeYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    viewModel: ScooterViewModel,
    telemetry: ScooterTelemetry,
    packetHistory: List<BlePacket>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Renifleur BLE, 1 = Codes Erreur, 2 = Architecture GATT

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
                        text = "DIAGNOSTIC & ANALYSE BLE",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Protocole Brightway UART / BLE de la NAVEE NT5 Max.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedTab == 0 && packetHistory.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearPacketLogs() },
                        modifier = Modifier.testTag("btn_clear_packets")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Effacer les trames", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Subtabs
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NaveeCyan,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Trames BLE (${packetHistory.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Codes Défaut", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Services GATT", fontSize = 12.sp) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // SNIFFER DE TRAMES BLE
                if (packetHistory.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "En attente de paquets...\nLes trames TX/RX apparaîtront ici en temps réel.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(packetHistory, key = { it.id }) { packet ->
                        PacketLogItem(packet = packet)
                    }
                }
            }

            1 -> {
                // ANNUAIRE DES CODES ERREURS BRIGHTWAY
                item {
                    val activeError = ScooterErrorDirectory.getError(telemetry.activeErrorCode)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (telemetry.activeErrorCode != 0) NaveeRed.copy(alpha = 0.15f) else NaveeGreen.copy(alpha = 0.15f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (telemetry.activeErrorCode != 0) NaveeRed else NaveeGreen
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (telemetry.activeErrorCode != 0) NaveeRed else NaveeGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeError.displayCode,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "État Actuel : ${activeError.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeError.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Répertoire des codes d'erreurs contrôleur ESC",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(ScooterErrorDirectory.errorMap.values.toList(), key = { it.code }) { err ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${err.displayCode} · ${err.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (err.code == 0) NaveeGreen else NaveeOrange
                                )
                            }
                            Text(
                                text = err.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Action : ${err.recommendedAction}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaveeCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            2 -> {
                // ARCHITECTURE SERVICES & CARACTÉRISTIQUES GATT
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Cartographie GATT Reverse-Engineerée",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Identifiée d'après les dépôts pepperonas/navee-st3-pro, scooterteam et décompilation Mi Home/Brightway.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            GattDetailRow(
                                label = "Service Principal Brightway",
                                uuid = NaveeBleProtocol.BRIGHTWAY_SERVICE_UUID.toString(),
                                role = "Service de contrôle et télémétrie"
                            )
                            GattDetailRow(
                                label = "Caractéristique TX (Écriture)",
                                uuid = NaveeBleProtocol.BRIGHTWAY_TX_CHAR_UUID.toString(),
                                role = "Write without response (0x6E, 0x64, 0x65, 0x66)"
                            )
                            GattDetailRow(
                                label = "Caractéristique RX (Notifications)",
                                uuid = NaveeBleProtocol.BRIGHTWAY_RX_CHAR_UUID.toString(),
                                role = "Notify avec descripteur 0x2902"
                            )
                            GattDetailRow(
                                label = "Handshake Auth Nonce",
                                uuid = NaveeBleProtocol.BRIGHTWAY_AUTH_READ_UUID.toString(),
                                role = "Lecture jeton aléatoire 20 octets"
                            )
                            GattDetailRow(
                                label = "Handshake Auth Réponse",
                                uuid = NaveeBleProtocol.BRIGHTWAY_AUTH_WRITE_UUID.toString(),
                                role = "Écriture challenge déchiffré"
                            )
                            GattDetailRow(
                                label = "Fallback Nordic UART (NUS)",
                                uuid = NaveeBleProtocol.NORDIC_UART_SERVICE_UUID.toString(),
                                role = "Mode port série générique 19200 bauds"
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PacketLogItem(packet: BlePacket) {
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    val timeStr = timeFormat.format(Date(packet.timestamp))
    val isTx = packet.direction == PacketDirection.TX

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isTx) NaveeCyan.copy(alpha = 0.3f) else NaveeGreen.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isTx) NaveeCyan.copy(alpha = 0.2f) else NaveeGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isTx) "TX ➔" else "RX ⬅",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isTx) NaveeCyan else NaveeGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = packet.commandName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            // Raw Hex Dump
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF090D14),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = packet.hexString,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (isTx) NaveeCyan else NaveeGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Text(
                text = packet.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun GattDetailRow(label: String, uuid: String, role: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NaveeCyan)
        Text(text = uuid, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = role, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
