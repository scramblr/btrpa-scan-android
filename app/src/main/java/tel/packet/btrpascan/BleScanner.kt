package tel.packet.btrpascan

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages BLE scanning with support for IRK resolution, RSSI averaging,
 * distance estimation, and various scan modes.
 */
class BleScanner(private val context: Context) {
    
    // Scan configuration
    data class ScanConfig(
        val targetMac: String? = null,
        val irk: ByteArray? = null,
        val minRssi: Int? = null,
        val rssiWindow: Int = 1,
        val environment: DistanceEstimator.Environment = DistanceEstimator.Environment.FREE_SPACE,
        val alertWithin: Double? = null,
        val activeScanning: Boolean = false,
        val timeout: Long? = 30_000L // null = infinite
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ScanConfig) return false
            if (targetMac != other.targetMac) return false
            if (irk != null) {
                if (other.irk == null) return false
                if (!irk.contentEquals(other.irk)) return false
            } else if (other.irk != null) return false
            if (minRssi != other.minRssi) return false
            if (rssiWindow != other.rssiWindow) return false
            if (environment != other.environment) return false
            if (alertWithin != other.alertWithin) return false
            if (activeScanning != other.activeScanning) return false
            if (timeout != other.timeout) return false
            return true
        }

        override fun hashCode(): Int {
            var result = targetMac?.hashCode() ?: 0
            result = 31 * result + (irk?.contentHashCode() ?: 0)
            result = 31 * result + (minRssi ?: 0)
            result = 31 * result + rssiWindow
            result = 31 * result + environment.hashCode()
            result = 31 * result + (alertWithin?.hashCode() ?: 0)
            result = 31 * result + activeScanning.hashCode()
            result = 31 * result + (timeout?.hashCode() ?: 0)
            return result
        }
    }
    
    // Scan statistics
    data class ScanStats(
        val totalDetections: Int = 0,
        val uniqueDevices: Int = 0,
        val irkMatches: Int = 0,
        val resolvedAddresses: Int = 0,
        val rpaCount: Int = 0,
        val elapsedMs: Long = 0
    )
    
    // Scan state
    sealed class ScanState {
        object Idle : ScanState()
        data class Scanning(val config: ScanConfig, val startTime: Long) : ScanState()
        object Stopping : ScanState()
        data class Error(val message: String) : ScanState()
    }
    
    // Callbacks
    interface ScanListener {
        fun onDeviceFound(device: BleDeviceInfo, isUpdate: Boolean)
        fun onIrkMatch(device: BleDeviceInfo)
        fun onTargetFound(device: BleDeviceInfo)
        fun onProximityAlert(device: BleDeviceInfo, distance: Double)
        fun onScanStateChanged(state: ScanState)
        fun onStatsUpdated(stats: ScanStats)
    }
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bleScanner: BluetoothLeScanner? = null
    
    private val handler = Handler(Looper.getMainLooper())
    
    // State
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    private val _devices = MutableStateFlow<Map<String, BleDeviceInfo>>(emptyMap())
    val devices: StateFlow<Map<String, BleDeviceInfo>> = _devices.asStateFlow()
    
    private val _stats = MutableStateFlow(ScanStats())
    val stats: StateFlow<ScanStats> = _stats.asStateFlow()
    
    // Internal tracking
    private val deviceMap = ConcurrentHashMap<String, BleDeviceInfo>()
    private val rssiHistory = ConcurrentHashMap<String, ArrayDeque<Int>>()
    private val resolvedAddresses = ConcurrentHashMap<String, Int>()
    
    private var currentConfig: ScanConfig? = null
    private var listener: ScanListener? = null
    private var totalDetections = 0
    private var irkMatches = 0
    private var rpaCount = 0
    private var scanStartTime = 0L
    
    fun setListener(listener: ScanListener?) {
        this.listener = listener
    }
    
    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true
    
    val isScanning: Boolean
        get() = _scanState.value is ScanState.Scanning
    
    @SuppressLint("MissingPermission")
    fun startScan(config: ScanConfig): Boolean {
        if (!isBluetoothEnabled) {
            _scanState.value = ScanState.Error("Bluetooth is not enabled")
            return false
        }
        
        bleScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bleScanner == null) {
            _scanState.value = ScanState.Error("BLE Scanner not available")
            return false
        }
        
        // Reset state
        deviceMap.clear()
        rssiHistory.clear()
        resolvedAddresses.clear()
        totalDetections = 0
        irkMatches = 0
        rpaCount = 0
        scanStartTime = System.currentTimeMillis()
        currentConfig = config
        
        // Configure scan settings
        val settingsBuilder = ScanSettings.Builder()
            .setScanMode(
                if (config.activeScanning) ScanSettings.SCAN_MODE_LOW_LATENCY
                else ScanSettings.SCAN_MODE_BALANCED
            )
            .setReportDelay(0)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        }
        
        try {
            bleScanner?.startScan(null, settingsBuilder.build(), scanCallback)
            _scanState.value = ScanState.Scanning(config, scanStartTime)
            listener?.onScanStateChanged(_scanState.value)
            
            // Set timeout if configured
            config.timeout?.let { timeout ->
                handler.postDelayed({ stopScan() }, timeout)
            }
            
            return true
        } catch (e: Exception) {
            _scanState.value = ScanState.Error("Failed to start scan: ${e.message}")
            return false
        }
    }
    
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (_scanState.value !is ScanState.Scanning) return
        
        _scanState.value = ScanState.Stopping
        
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            // Ignore
        }
        
        handler.removeCallbacksAndMessages(null)
        
        updateStats()
        _scanState.value = ScanState.Idle
        listener?.onScanStateChanged(_scanState.value)
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { handleScanResult(it) }
        }
        
        override fun onScanFailed(errorCode: Int) {
            val message = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                else -> "Unknown error ($errorCode)"
            }
            _scanState.value = ScanState.Error(message)
            listener?.onScanStateChanged(_scanState.value)
        }
    }
    
    private fun handleScanResult(result: ScanResult) {
        val config = currentConfig ?: return
        
        val device = BleDeviceInfo.fromScanResult(result)
        val address = device.address.uppercase()
        
        // Track RPA count
        if (device.isRpa) {
            rpaCount++
        }
        
        // Calculate averaged RSSI
        val avgRssi = if (config.rssiWindow > 1) {
            updateRssiAverage(address, device.rssi, config.rssiWindow)
        } else null
        
        val effectiveRssi = avgRssi ?: device.rssi
        
        // RSSI filtering
        config.minRssi?.let { minRssi ->
            if (effectiveRssi < minRssi) return
        }
        
        // Calculate distance
        val distance = DistanceEstimator.estimateDistance(
            effectiveRssi,
            device.txPower,
            config.environment
        )
        
        totalDetections++
        
        // Handle different modes
        when {
            config.irk != null -> handleIrkMode(device, address, avgRssi, distance, config)
            config.targetMac != null -> handleTargetMode(device, address, avgRssi, distance, config)
            else -> handleDiscoverMode(device, address, avgRssi, distance, config)
        }
        
        updateStats()
    }
    
    private fun handleIrkMode(
        device: BleDeviceInfo,
        address: String,
        avgRssi: Int?,
        distance: Double?,
        config: ScanConfig
    ) {
        val existingDevice = deviceMap[address]
        val timesSeen = (existingDevice?.timesSeen ?: 0) + 1
        
        val resolved = IrkResolver.resolveRpa(config.irk!!, address)
        
        val updatedDevice = device.copy(
            avgRssi = avgRssi,
            estimatedDistance = distance,
            timesSeen = timesSeen,
            irkResolved = resolved
        )
        
        deviceMap[address] = updatedDevice
        _devices.value = deviceMap.toMap()
        
        if (resolved) {
            irkMatches++
            resolvedAddresses[address] = (resolvedAddresses[address] ?: 0) + 1
            listener?.onIrkMatch(updatedDevice)
        }
        
        listener?.onDeviceFound(updatedDevice, existingDevice != null)
        
        // Proximity alert
        checkProximityAlert(updatedDevice, distance, config)
    }
    
    private fun handleTargetMode(
        device: BleDeviceInfo,
        address: String,
        avgRssi: Int?,
        distance: Double?,
        config: ScanConfig
    ) {
        if (!address.contains(config.targetMac!!.uppercase())) return
        
        val existingDevice = deviceMap[address]
        val timesSeen = (existingDevice?.timesSeen ?: 0) + 1
        
        val updatedDevice = device.copy(
            avgRssi = avgRssi,
            estimatedDistance = distance,
            timesSeen = timesSeen
        )
        
        deviceMap[address] = updatedDevice
        _devices.value = deviceMap.toMap()
        
        listener?.onTargetFound(updatedDevice)
        listener?.onDeviceFound(updatedDevice, existingDevice != null)
        
        checkProximityAlert(updatedDevice, distance, config)
    }
    
    private fun handleDiscoverMode(
        device: BleDeviceInfo,
        address: String,
        avgRssi: Int?,
        distance: Double?,
        config: ScanConfig
    ) {
        val existingDevice = deviceMap[address]
        val timesSeen = (existingDevice?.timesSeen ?: 0) + 1
        
        val updatedDevice = device.copy(
            avgRssi = avgRssi,
            estimatedDistance = distance,
            timesSeen = timesSeen
        )
        
        deviceMap[address] = updatedDevice
        _devices.value = deviceMap.toMap()
        
        listener?.onDeviceFound(updatedDevice, existingDevice != null)
        
        checkProximityAlert(updatedDevice, distance, config)
    }
    
    private fun updateRssiAverage(address: String, rssi: Int, windowSize: Int): Int {
        val history = rssiHistory.getOrPut(address) { ArrayDeque() }
        history.addLast(rssi)
        while (history.size > windowSize) {
            history.removeFirst()
        }
        return history.average().toInt()
    }
    
    private fun checkProximityAlert(device: BleDeviceInfo, distance: Double?, config: ScanConfig) {
        val alertThreshold = config.alertWithin ?: return
        val dist = distance ?: return
        
        if (dist <= alertThreshold) {
            listener?.onProximityAlert(device, dist)
        }
    }
    
    private fun updateStats() {
        val uniqueRpaDevices = deviceMap.values.count { it.isRpa }
        _stats.value = ScanStats(
            totalDetections = totalDetections,
            uniqueDevices = deviceMap.size,
            irkMatches = irkMatches,
            resolvedAddresses = resolvedAddresses.size,
            rpaCount = uniqueRpaDevices,
            elapsedMs = System.currentTimeMillis() - scanStartTime
        )
        listener?.onStatsUpdated(_stats.value)
    }
    
    fun getResolvedAddresses(): Map<String, Int> = resolvedAddresses.toMap()
    
    fun getAllDevices(): List<BleDeviceInfo> = deviceMap.values.toList()
}
