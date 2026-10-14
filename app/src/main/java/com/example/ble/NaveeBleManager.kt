package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.model.BlePacket
import com.example.model.ConnectionStatus
import com.example.model.PacketDirection
import com.example.model.ScooterTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class DiscoveredBleDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val rssi: Int,
    val isNavee: Boolean = false
)

class NaveeBleManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _telemetry = MutableStateFlow(ScooterTelemetry())
    val telemetry: StateFlow<ScooterTelemetry> = _telemetry.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectingDeviceAddress = MutableStateFlow<String?>(null)
    val connectingDeviceAddress: StateFlow<String?> = _connectingDeviceAddress.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _packetLog = MutableSharedFlow<BlePacket>(extraBufferCapacity = 128)
    val packetLog: SharedFlow<BlePacket> = _packetLog.asSharedFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private var pollingJob: Job? = null
    private var connectionTimeoutJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    fun isNaveeDevice(name: String): Boolean {
        val upper = name.uppercase()
        return upper.contains("NAVEE") || upper.contains("BRIGHTWAY") || upper.startsWith("NV") || upper.contains("NT5")
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!isBluetoothEnabled) {
            _connectionError.value = "Le Bluetooth est désactivé sur votre téléphone."
            logPacket(
                BlePacket(
                    direction = PacketDirection.RX,
                    commandId = 0x00,
                    commandName = "SCAN_ERROR",
                    rawBytes = byteArrayOf(),
                    description = "Bluetooth désactivé sur le smartphone",
                    isSuccess = false
                )
            )
            return
        }

        _connectionError.value = null
        _connectionStatus.value = ConnectionStatus.SCANNING
        _discoveredDevices.value = emptyList()

        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: run {
            _connectionError.value = "Scanner BLE indisponible sur cet appareil."
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
            logPacket(
                BlePacket(
                    direction = PacketDirection.TX,
                    commandId = 0x00,
                    commandName = "BLE_SCAN_START",
                    rawBytes = byteArrayOf(),
                    description = "Scan BLE actif (Recherche de trottinettes NAVEE)"
                )
            )
        } catch (e: Exception) {
            _connectionError.value = "Erreur lancement scan: ${e.message}"
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        if (_connectionStatus.value == ConnectionStatus.SCANNING) {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = result.scanRecord?.deviceName ?: device.name ?: "Périphérique Inconnu"
            val address = device.address
            val rssi = result.rssi
            val isNavee = isNaveeDevice(name)

            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == address }

            val item = DiscoveredBleDevice(device, name, address, rssi, isNavee)
            if (existingIndex >= 0) {
                currentList[existingIndex] = item
            } else {
                currentList.add(item)
            }
            _discoveredDevices.value = currentList
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            _connectionError.value = "Échec du scan BLE (code $errorCode)"
            logPacket(
                BlePacket(
                    direction = PacketDirection.RX,
                    commandId = 0x00,
                    commandName = "SCAN_FAILED",
                    rawBytes = byteArrayOf(errorCode.toByte()),
                    description = "Échec du scan BLE (Code $errorCode)",
                    isSuccess = false
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        stopScan()
        _connectionError.value = null
        _connectingDeviceAddress.value = device.address
        _connectionStatus.value = ConnectionStatus.CONNECTING

        _telemetry.value = _telemetry.value.copy(
            deviceName = device.name ?: "NAVEE NT5 Max",
            deviceMac = device.address,
            connectionStatus = ConnectionStatus.CONNECTING
        )

        logPacket(
            BlePacket(
                direction = PacketDirection.TX,
                commandId = 0x00,
                commandName = "GATT_CONNECT_REQ",
                rawBytes = byteArrayOf(),
                description = "Tentative de connexion GATT vers ${device.name ?: device.address}..."
            )
        )

        // Lancer la connexion sur un thread IO avec un court délai après l'arrêt du scan
        scope.launch(Dispatchers.IO) {
            delay(200) // Laisse le contrôleur Bluetooth libérer le slot de scan

            // Fermer proprement toute instance GATT précédente
            try {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
            } catch (_: Exception) {}
            bluetoothGatt = null

            // Armer un timeout de 12 secondes
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = scope.launch {
                delay(12000)
                if (_connectionStatus.value == ConnectionStatus.CONNECTING) {
                    _connectionError.value = "Délai de connexion dépassé. Vérifiez que la trottinette n'est pas déjà connectée à l'application officielle NAVEE."
                    disconnect()
                }
            }

            try {
                bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    device.connectGatt(context.applicationContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                } else {
                    device.connectGatt(context.applicationContext, false, gattCallback)
                }

                if (bluetoothGatt == null) {
                    _connectionError.value = "Impossible d'initialiser la connexion GATT."
                    disconnect()
                }
            } catch (e: Exception) {
                _connectionError.value = "Exception Bluetooth : ${e.message}"
                disconnect()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = null
        pollingJob?.cancel()
        pollingJob = null

        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {}
        bluetoothGatt = null
        writeCharacteristic = null
        notifyCharacteristic = null

        _connectingDeviceAddress.value = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _telemetry.value = _telemetry.value.copy(
            connectionStatus = ConnectionStatus.DISCONNECTED,
            speedKmh = 0.0f
        )
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            connectionTimeoutJob?.cancel()

            if (status != BluetoothGatt.GATT_SUCCESS) {
                val errorMsg = when (status) {
                    133 -> "Erreur GATT 133 (Conflit pile Bluetooth). Redémarrez le Bluetooth ou réinitialisez l'appairage de la trottinette."
                    19 -> "Erreur GATT 19 (Connexion interrompue par la trottinette)."
                    8 -> "Erreur GATT 8 (Délai dépassé)."
                    else -> "Erreur de connexion Bluetooth (Statut GATT $status)."
                }
                _connectionError.value = errorMsg
                logPacket(
                    BlePacket(
                        direction = PacketDirection.RX,
                        commandId = 0x00,
                        commandName = "GATT_STATUS_ERR",
                        rawBytes = byteArrayOf(status.toByte()),
                        description = errorMsg,
                        isSuccess = false
                    )
                )
                disconnect()
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _connectionError.value = null
                _telemetry.value = _telemetry.value.copy(connectionStatus = ConnectionStatus.CONNECTED)

                logPacket(
                    BlePacket(
                        direction = PacketDirection.RX,
                        commandId = 0x00,
                        commandName = "GATT_CONNECTED",
                        rawBytes = byteArrayOf(),
                        description = "Connexion physique établie avec succès. Découverte des services..."
                    )
                )

                // Lancer la découverte des services après un court délai pour laisser la connexion se stabiliser
                mainHandler.postDelayed({
                    try {
                        gatt.discoverServices()
                    } catch (e: Exception) {
                        _connectionError.value = "Erreur découverte services : ${e.message}"
                    }
                }, 300)

                // Demander l'extension MTU de manière non bloquante
                try {
                    gatt.requestMtu(512)
                } catch (_: Exception) {}

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                logPacket(
                    BlePacket(
                        direction = PacketDirection.RX,
                        commandId = 0x00,
                        commandName = "GATT_DISCONNECTED",
                        rawBytes = byteArrayOf(),
                        description = "Trottinette déconnectée."
                    )
                )
                disconnect()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            logPacket(
                BlePacket(
                    direction = PacketDirection.RX,
                    commandId = 0x00,
                    commandName = "MTU_OK",
                    rawBytes = byteArrayOf(),
                    description = "MTU négocié : $mtu octets"
                )
            )
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionError.value = "Échec de lecture des services GATT (Statut $status)"
                return
            }

            val services = gatt.services
            logPacket(
                BlePacket(
                    direction = PacketDirection.RX,
                    commandId = 0x00,
                    commandName = "SERVICES_FOUND",
                    rawBytes = byteArrayOf(services.size.toByte()),
                    description = "${services.size} services découverts sur la NAVEE NT5 Max."
                )
            )

            // 1. Essai avec le service Brightway spécifique
            var service = gatt.getService(NaveeBleProtocol.BRIGHTWAY_SERVICE_UUID)
            if (service != null) {
                writeCharacteristic = service.getCharacteristic(NaveeBleProtocol.BRIGHTWAY_TX_CHAR_UUID)
                notifyCharacteristic = service.getCharacteristic(NaveeBleProtocol.BRIGHTWAY_RX_CHAR_UUID)
            }

            // 2. Essai avec le standard Nordic UART Service (NUS)
            if (writeCharacteristic == null || notifyCharacteristic == null) {
                service = gatt.getService(NaveeBleProtocol.NORDIC_UART_SERVICE_UUID)
                if (service != null) {
                    writeCharacteristic = service.getCharacteristic(NaveeBleProtocol.NORDIC_UART_TX_CHAR_UUID)
                    notifyCharacteristic = service.getCharacteristic(NaveeBleProtocol.NORDIC_UART_RX_CHAR_UUID)
                }
            }

            // 3. Fallback dynamique : recherche automatique des caractéristiques Write et Notify
            if (writeCharacteristic == null || notifyCharacteristic == null) {
                for (s in services) {
                    for (c in s.characteristics) {
                        val props = c.properties
                        val canWrite = (props and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                        val canNotify = (props and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0

                        if (canWrite && writeCharacteristic == null) {
                            writeCharacteristic = c
                        }
                        if (canNotify && notifyCharacteristic == null) {
                            notifyCharacteristic = c
                        }
                    }
                }
            }

            if (notifyCharacteristic != null) {
                gatt.setCharacteristicNotification(notifyCharacteristic, true)
                val descriptor = notifyCharacteristic?.getDescriptor(CCCD_UUID)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }

                logPacket(
                    BlePacket(
                        direction = PacketDirection.RX,
                        commandId = 0x00,
                        commandName = "CANAL_COMM_PRET",
                        rawBytes = byteArrayOf(),
                        description = "Canal de communication NAVEE opérationnel (Écriture & Télémétrie actives)."
                    )
                )

                // Démarrer l'interrogation continue de télémétrie
                startTelemetryPolling()
            } else {
                _connectionError.value = "Caractéristique de notification introuvable sur l'appareil."
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handleIncomingBytes(characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingBytes(value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logPacket(
                    BlePacket(
                        direction = PacketDirection.RX,
                        commandId = 0x00,
                        commandName = "WRITE_FAILED",
                        rawBytes = byteArrayOf(status.toByte()),
                        description = "Erreur transmission trame (Statut $status)",
                        isSuccess = false
                    )
                )
            }
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray) {
        val result = NaveeBleProtocol.parseResponsePacket(bytes, _telemetry.value)
        if (result != null) {
            _telemetry.value = result.first
            logPacket(result.second)
        }
    }

    private fun startTelemetryPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive && _connectionStatus.value == ConnectionStatus.CONNECTED) {
                val queryPacket = NaveeBleProtocol.buildQueryTelemetryPacket()
                sendBlePacket(queryPacket)
                delay(300) // Fréquence de rafraîchissement 3.3 Hz
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendBlePacket(packet: BlePacket) {
        val gatt = bluetoothGatt ?: return
        val char = writeCharacteristic ?: return

        char.value = packet.rawBytes
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        gatt.writeCharacteristic(char)

        logPacket(packet)
    }

    fun sendSpeedLimit(speedKmh: Int) {
        _telemetry.value = _telemetry.value.copy(targetSpeedLimit = speedKmh)
        sendBlePacket(NaveeBleProtocol.buildSetSpeedLimitPacket(speedKmh))
    }

    fun sendKickStart(speedKmh: Int) {
        sendBlePacket(NaveeBleProtocol.buildSetKickStartPacket(speedKmh))
    }

    fun sendLock(locked: Boolean) {
        sendBlePacket(NaveeBleProtocol.buildSetLockPacket(locked))
    }

    fun sendLights(lightOn: Boolean) {
        sendBlePacket(NaveeBleProtocol.buildSetLightsPacket(lightOn))
    }

    private fun logPacket(packet: BlePacket) {
        scope.launch {
            _packetLog.emit(packet)
        }
    }
}
