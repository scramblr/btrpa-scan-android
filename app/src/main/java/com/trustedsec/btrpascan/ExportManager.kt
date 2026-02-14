package com.trustedsec.btrpascan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for exporting scan results to various formats.
 */
object ExportManager {
    
    enum class ExportFormat(val extension: String, val mimeType: String) {
        CSV("csv", "text/csv"),
        JSON("json", "application/json"),
        JSONL("jsonl", "application/x-ndjson")
    }
    
    /**
     * Export devices to a file and return the file URI.
     */
    fun exportToFile(
        context: Context,
        devices: List<BleDeviceInfo>,
        format: ExportFormat
    ): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "btrpa-scan-$timestamp.${format.extension}"
        
        val file = File(context.cacheDir, fileName)
        
        try {
            FileWriter(file).use { writer ->
                when (format) {
                    ExportFormat.CSV -> writeCsv(writer, devices)
                    ExportFormat.JSON -> writeJson(writer, devices)
                    ExportFormat.JSONL -> writeJsonl(writer, devices)
                }
            }
            
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun writeCsv(writer: FileWriter, devices: List<BleDeviceInfo>) {
        // Header
        writer.write("timestamp,address,name,rssi,avg_rssi,tx_power,est_distance,manufacturer_data,service_uuids,resolved\n")
        
        devices.forEach { device ->
            val row = listOf(
                device.timestamp,
                device.address,
                escapeCsv(device.displayName),
                device.rssi.toString(),
                device.avgRssi?.toString() ?: "",
                device.txPower?.toString() ?: "",
                device.estimatedDistance?.let { String.format("%.2f", it) } ?: "",
                escapeCsv(device.manufacturerDataHex),
                escapeCsv(device.serviceUuids?.joinToString(", ") ?: ""),
                device.irkResolved?.toString() ?: ""
            ).joinToString(",")
            
            writer.write("$row\n")
        }
    }
    
    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
    
    private fun writeJson(writer: FileWriter, devices: List<BleDeviceInfo>) {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
        
        val records = devices.map { device ->
            mapOf(
                "timestamp" to device.timestamp,
                "address" to device.address,
                "name" to device.displayName,
                "rssi" to device.rssi,
                "avg_rssi" to device.avgRssi,
                "tx_power" to device.txPower,
                "est_distance" to device.estimatedDistance?.let { String.format("%.2f", it) },
                "manufacturer_data" to device.manufacturerDataHex.ifEmpty { null },
                "service_uuids" to device.serviceUuids,
                "resolved" to device.irkResolved
            )
        }
        
        writer.write(gson.toJson(records))
    }
    
    private fun writeJsonl(writer: FileWriter, devices: List<BleDeviceInfo>) {
        val gson = GsonBuilder().create()
        
        devices.forEach { device ->
            val record = mapOf(
                "timestamp" to device.timestamp,
                "address" to device.address,
                "name" to device.displayName,
                "rssi" to device.rssi,
                "avg_rssi" to device.avgRssi,
                "tx_power" to device.txPower,
                "est_distance" to device.estimatedDistance?.let { String.format("%.2f", it) },
                "manufacturer_data" to device.manufacturerDataHex.ifEmpty { null },
                "service_uuids" to device.serviceUuids,
                "resolved" to device.irkResolved
            )
            writer.write("${gson.toJson(record)}\n")
        }
    }
    
    /**
     * Share the exported file via Android share sheet.
     */
    fun shareFile(context: Context, uri: Uri, format: ExportFormat): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
