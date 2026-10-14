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
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
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
    val rssi: Int
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

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _packetLog = MutableSharedFlow<BlePacket>(extraBufferCapacity = 128)
    val packetLog: SharedFlow<BlePacket> = _packetLog.asSharedFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private var pollingJob: Job? = null
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan(onlyNavee: Boolean = false) {
        if (!isBluetoothEnabled) {
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

        _connectionStatus.value = ConnectionStatus.SCANNING
        _discoveredDevices.value = emptyList()

        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = mutableListOf<ScanFilter>()
        if (onlyNavee) {
            filters.add(ScanFilter.Builder().setDeviceName("NAVEE").build())
            filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(NaveeBleProtocol.BRIGHTWAY_SERVICE_UUID)).build())
            filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(NaveeBleProtocol.NORDIC_UART_SERVICE_UUID)).build())
        }

        try {
            scanner.startScan(if (onlyNavee) filters else null, settings, scanCallback)
            logPacket(
                BlePacket(
                    direction = PacketDirection.TX,
                    commandId = 0x00,
                    commandName = "BLE_SCAN_START",
                    rawBytes = byteArrayOf(),
                    description = "Scan BLE lancé (Recherche trottinettes NAVEE / Brightway)"
                )
            )
        } catch (e: Exception) {
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

            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == address }

            val item = DiscoveredBleDevice(device, name, address, rssi)
            if (existingIndex >= 0) {
                currentList[existingIndex] = item
            } else {
                currentList.add(item)
            }
            _discoveredDevices.value = currentList
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
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
                commandName = "GATT_CONNECT",
                rawBytes = byteArrayOf(),
                description = "Connexion GATT en cours vers ${device.name ?: device.address}"
            )
        )

        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null
        notifyCharacteristic = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _telemetry.value = _telemetry.value.copy(
            connectionStatus = ConnectionStatus.DISCONNECTED,
            speedKmh = 0.0f
        )
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _telemetry.value = _telemetry.value.copy(connectionStatus = ConnectionStatus.CONNECTED)
                logPacket(
                    BlePacket(
                        direction = PacketDirection.RX,
                        commandId = 0x00,
                        commandName = "GATT_CONNECTED",
                        rawBytes = byteArrayOf(),
                        description = "GATT connecté. Négociation MTU 512..."
                    )
                )
                gatt.requestMtu(512)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnect()
                logPacket(
                    BlePacket(
                        direction = PacketDirection.RX,
                        commandId = 0x00,
                        commandName = "GATT_DISCONNECTED",
                        rawBytes = byteArrayOf(),
                        description = "Trottinette déconnectée"
                    )
                )
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            logPacket(
                BlePacket(
                    direction = PacketDirection.RX,
                    commandId = 0x00,
                    commandName = "MTU_NEGOTIATED",
                    rawBytes = byteArrayOf(),
                    description = "MTU négocié : $mtu octets. Découverte des services..."
                )
            )
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            // Cherche le service Brightway ou Nordic UART
            var service = gatt.getService(NaveeBleProtocol.BRIGHTWAY_SERVICE_UUID)
            if (service != null) {
                writeCharacteristic = service.getCharacteristic(NaveeBleProtocol.BRIGHTWAY_TX_CHAR_UUID)
                notifyCharacteristic = service.getCharacteristic(NaveeBleProtocol.BRIGHTWAY_RX_CHAR_UUID)
            } else {
                service = gatt.getService(NaveeBleProtocol.NORDIC_UART_SERVICE_UUID)
                if (service != null) {
                    writeCharacteristic = service.getCharacteristic(NaveeBleProtocol.NORDIC_UART_TX_CHAR_UUID)
                    notifyCharacteristic = service.getCharacteristic(NaveeBleProtocol.NORDIC_UART_RX_CHAR_UUID)
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
                        commandName = "SERVICES_READY",
                        rawBytes = byteArrayOf(),
                        description = "Service NAVEE initialisé et notifications activées."
                    )
                )
                startTelemetryPolling()
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
                        description = "Échec d'écriture caractéristique GATT (Statut $status)",
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
                delay(300) // Poll 3.3 Hz
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
