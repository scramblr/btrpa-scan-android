package tel.packet.btrpascan

import android.bluetooth.le.ScanResult
import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents a discovered BLE device with all relevant scan data.
 */
data class BleDeviceInfo(
    val address: String,
    val name: String?,
    val rssi: Int,
    var avgRssi: Int? = null,
    val txPower: Int?,
    var estimatedDistance: Double? = null,
    val manufacturerData: Map<Int, ByteArray>?,
    val serviceUuids: List<String>?,
    var timesSeen: Int = 1,
    var lastSeen: Long = System.currentTimeMillis(),
    var irkResolved: Boolean? = null,
    var isRpa: Boolean = false
) {
    val timestamp: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(lastSeen))
    
    val lastSeenFormatted: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(lastSeen))
    
    val manufacturerDataHex: String
        get() = manufacturerData?.entries?.joinToString("; ") { (id, data) ->
            "0x${id.toString(16).padStart(4, '0').uppercase()}:${data.toHexString()}"
        } ?: ""
    
    val displayName: String
        get() = name ?: "Unknown"
    
    /**
     * Returns the address type indicator for display
     */
    val addressTypeLabel: String
        get() = when {
            isRpa -> "RPA"
            else -> "Public/Static"
        }
    
    companion object {
        fun fromScanResult(result: ScanResult): BleDeviceInfo {
            val device = result.device
            val record = result.scanRecord
            
            // Extract manufacturer data
            val mfrData = record?.manufacturerSpecificData?.let { sparseArray ->
                val map = mutableMapOf<Int, ByteArray>()
                for (i in 0 until sparseArray.size()) {
                    map[sparseArray.keyAt(i)] = sparseArray.valueAt(i)
                }
                map
            }
            
            // Extract service UUIDs
            val uuids = record?.serviceUuids?.map { it.toString() }
            
            // Check if address is an RPA
            val address = device.address
            val isRpa = IrkResolver.isRpa(address)
            
            return BleDeviceInfo(
                address = address,
                name = record?.deviceName ?: device.name,
                rssi = result.rssi,
                txPower = record?.txPowerLevel?.takeIf { it != Int.MIN_VALUE },
                manufacturerData = mfrData,
                serviceUuids = uuids,
                isRpa = isRpa
            )
        }
    }
}

fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
