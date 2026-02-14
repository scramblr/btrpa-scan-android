package com.trustedsec.btrpascan

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), BleScanner.ScanListener {
    
    private lateinit var scanner: BleScanner
    private lateinit var adapter: DeviceAdapter
    
    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabScan: FloatingActionButton
    private lateinit var statsBar: LinearLayout
    private lateinit var textDevices: TextView
    private lateinit var textDetections: TextView
    private lateinit var textElapsed: TextView
    private lateinit var textIrkMatches: TextView
    private lateinit var emptyView: TextView
    private lateinit var modeChipGroup: ChipGroup
    
    // Current configuration
    private var currentMode = ScanMode.DISCOVER_ALL
    private var targetMac: String? = null
    private var irk: ByteArray? = null
    private var minRssi: Int? = null
    private var rssiWindow = 1
    private var environment = DistanceEstimator.Environment.FREE_SPACE
    private var alertWithin: Double? = null
    private var activeScanning = false
    private var timeout: Long? = 30_000L
    
    enum class ScanMode {
        DISCOVER_ALL,
        TARGET_MAC,
        IRK_RESOLVE
    }
    
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startScanning()
        } else {
            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show()
        }
    }
    
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startScanning()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "BTRPA-Scan"
        
        // Initialize scanner
        scanner = BleScanner(this)
        scanner.setListener(this)
        
        // Initialize views
        setupViews()
        setupRecyclerView()
        setupModeChips()
        observeScanner()
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerDevices)
        fabScan = findViewById(R.id.fabScan)
        statsBar = findViewById(R.id.statsBar)
        textDevices = findViewById(R.id.textDevices)
        textDetections = findViewById(R.id.textDetections)
        textElapsed = findViewById(R.id.textElapsed)
        textIrkMatches = findViewById(R.id.textIrkMatches)
        emptyView = findViewById(R.id.emptyView)
        modeChipGroup = findViewById(R.id.modeChipGroup)
        
        fabScan.setOnClickListener {
            if (scanner.isScanning) {
                scanner.stopScan()
            } else {
                checkPermissionsAndScan()
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = DeviceAdapter { device ->
            showDeviceDetails(device)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupModeChips() {
        val chipDiscover: Chip = findViewById(R.id.chipDiscover)
        val chipTarget: Chip = findViewById(R.id.chipTarget)
        val chipIrk: Chip = findViewById(R.id.chipIrk)
        
        chipDiscover.setOnClickListener {
            currentMode = ScanMode.DISCOVER_ALL
            targetMac = null
            irk = null
            textIrkMatches.visibility = View.GONE
        }
        
        chipTarget.setOnClickListener {
            showTargetMacDialog()
        }
        
        chipIrk.setOnClickListener {
            showIrkDialog()
        }
    }
    
    private fun showTargetMacDialog() {
        val input = EditText(this).apply {
            hint = "XX:XX:XX:XX:XX:XX"
            inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        
        AlertDialog.Builder(this)
            .setTitle("Target MAC Address")
            .setMessage("Enter the MAC address to search for:")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val mac = input.text.toString().trim()
                if (IrkResolver.isValidMacAddress(mac)) {
                    currentMode = ScanMode.TARGET_MAC
                    targetMac = mac
                    irk = null
                    textIrkMatches.visibility = View.GONE
                    Toast.makeText(this, "Target: $mac", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid MAC address format", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showIrkDialog() {
        val input = EditText(this).apply {
            hint = "32 hex characters (e.g., 0x...)"
            inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        
        AlertDialog.Builder(this)
            .setTitle("Identity Resolving Key (IRK)")
            .setMessage("Enter the 16-byte IRK in hex format:")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val irkString = input.text.toString().trim()
                val error = IrkResolver.validateIrk(irkString)
                if (error == null) {
                    currentMode = ScanMode.IRK_RESOLVE
                    irk = IrkResolver.parseIrk(irkString)
                    targetMac = null
                    textIrkMatches.visibility = View.VISIBLE
                    timeout = null // Infinite for IRK mode
                    Toast.makeText(this, "IRK mode enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun observeScanner() {
        lifecycleScope.launch {
            scanner.devices.collectLatest { devices ->
                val sortedDevices = devices.values
                    .sortedByDescending { it.rssi }
                    .toList()
                
                adapter.submitList(sortedDevices)
                
                emptyView.visibility = if (sortedDevices.isEmpty() && scanner.isScanning) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
        
        lifecycleScope.launch {
            scanner.scanState.collectLatest { state ->
                when (state) {
                    is BleScanner.ScanState.Scanning -> {
                        fabScan.setImageResource(R.drawable.ic_stop)
                        statsBar.visibility = View.VISIBLE
                        modeChipGroup.isEnabled = false
                    }
                    is BleScanner.ScanState.Idle -> {
                        fabScan.setImageResource(R.drawable.ic_bluetooth_scan)
                        modeChipGroup.isEnabled = true
                    }
                    is BleScanner.ScanState.Error -> {
                        Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                        fabScan.setImageResource(R.drawable.ic_bluetooth_scan)
                    }
                    else -> {}
                }
            }
        }
        
        lifecycleScope.launch {
            scanner.stats.collectLatest { stats ->
                textDevices.text = "Devices: ${stats.uniqueDevices}"
                textDetections.text = "Detections: ${stats.totalDetections}"
                textElapsed.text = "Time: ${stats.elapsedMs / 1000}s"
                
                if (currentMode == ScanMode.IRK_RESOLVE) {
                    textIrkMatches.text = "IRK Matches: ${stats.irkMatches}"
                }
            }
        }
    }
    
    private fun checkPermissionsAndScan() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            startScanning()
        }
    }
    
    private fun startScanning() {
        if (!scanner.isBluetoothEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableIntent)
            return
        }
        
        val config = BleScanner.ScanConfig(
            targetMac = targetMac,
            irk = irk,
            minRssi = minRssi,
            rssiWindow = rssiWindow,
            environment = environment,
            alertWithin = alertWithin,
            activeScanning = activeScanning,
            timeout = timeout
        )
        
        scanner.startScan(config)
    }
    
    private fun showDeviceDetails(device: BleDeviceInfo) {
        val message = buildString {
            appendLine("Address: ${device.address}")
            appendLine("Name: ${device.displayName}")
            appendLine("RSSI: ${device.rssi} dBm")
            device.avgRssi?.let { appendLine("Avg RSSI: $it dBm") }
            device.txPower?.let { appendLine("TX Power: $it dBm") }
            device.estimatedDistance?.let { 
                appendLine("Est. Distance: ${DistanceEstimator.formatDistance(it)}") 
            }
            appendLine("Times Seen: ${device.timesSeen}")
            appendLine("Last Seen: ${device.lastSeenFormatted}")
            
            if (device.manufacturerDataHex.isNotEmpty()) {
                appendLine("\nManufacturer Data:")
                appendLine(device.manufacturerDataHex)
            }
            
            device.serviceUuids?.let { uuids ->
                if (uuids.isNotEmpty()) {
                    appendLine("\nService UUIDs:")
                    uuids.forEach { appendLine("  $it") }
                }
            }
            
            device.irkResolved?.let { resolved ->
                appendLine("\nRPA Status: ${if (resolved) "IRK MATCHED ✓" else "Not resolved"}")
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Device Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showSettingsDialog()
                true
            }
            R.id.action_export_csv -> {
                exportResults(ExportManager.ExportFormat.CSV)
                true
            }
            R.id.action_export_json -> {
                exportResults(ExportManager.ExportFormat.JSON)
                true
            }
            R.id.action_clear -> {
                adapter.submitList(emptyList())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        
        val switchActive = dialogView.findViewById<SwitchMaterial>(R.id.switchActiveScanning)
        val editMinRssi = dialogView.findViewById<EditText>(R.id.editMinRssi)
        val editRssiWindow = dialogView.findViewById<EditText>(R.id.editRssiWindow)
        val editAlertWithin = dialogView.findViewById<EditText>(R.id.editAlertWithin)
        val editTimeout = dialogView.findViewById<EditText>(R.id.editTimeout)
        val chipGroupEnv = dialogView.findViewById<ChipGroup>(R.id.chipGroupEnvironment)
        
        // Set current values
        switchActive.isChecked = activeScanning
        minRssi?.let { editMinRssi.setText(it.toString()) }
        editRssiWindow.setText(rssiWindow.toString())
        alertWithin?.let { editAlertWithin.setText(it.toString()) }
        timeout?.let { editTimeout.setText((it / 1000).toString()) }
        
        when (environment) {
            DistanceEstimator.Environment.FREE_SPACE -> 
                chipGroupEnv.check(R.id.chipFreeSpace)
            DistanceEstimator.Environment.OUTDOOR -> 
                chipGroupEnv.check(R.id.chipOutdoor)
            DistanceEstimator.Environment.INDOOR -> 
                chipGroupEnv.check(R.id.chipIndoor)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Scan Settings")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                activeScanning = switchActive.isChecked
                minRssi = editMinRssi.text.toString().toIntOrNull()
                rssiWindow = editRssiWindow.text.toString().toIntOrNull() ?: 1
                alertWithin = editAlertWithin.text.toString().toDoubleOrNull()
                timeout = editTimeout.text.toString().toLongOrNull()?.times(1000)
                
                environment = when (chipGroupEnv.checkedChipId) {
                    R.id.chipFreeSpace -> DistanceEstimator.Environment.FREE_SPACE
                    R.id.chipOutdoor -> DistanceEstimator.Environment.OUTDOOR
                    R.id.chipIndoor -> DistanceEstimator.Environment.INDOOR
                    else -> DistanceEstimator.Environment.FREE_SPACE
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exportResults(format: ExportManager.ExportFormat) {
        val devices = scanner.getAllDevices()
        if (devices.isEmpty()) {
            Toast.makeText(this, "No devices to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        val uri = ExportManager.exportToFile(this, devices, format)
        if (uri != null) {
            val shareIntent = ExportManager.shareFile(this, uri, format)
            startActivity(Intent.createChooser(shareIntent, "Export Results"))
        } else {
            Toast.makeText(this, "Export failed", Toast.LENGTH_LONG).show()
        }
    }
    
    // BleScanner.ScanListener implementation
    
    override fun onDeviceFound(device: BleDeviceInfo, isUpdate: Boolean) {
        // UI updates handled by Flow collection
    }
    
    override fun onIrkMatch(device: BleDeviceInfo) {
        // Vibrate for IRK match
        vibrate(100)
    }
    
    override fun onTargetFound(device: BleDeviceInfo) {
        // Vibrate for target found
        vibrate(200)
        Toast.makeText(this, "Target found: ${device.address}", Toast.LENGTH_SHORT).show()
    }
    
    override fun onProximityAlert(device: BleDeviceInfo, distance: Double) {
        vibrate(500)
        Toast.makeText(
            this, 
            "Proximity Alert: ${device.displayName} within ${String.format("%.1f", distance)}m",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    override fun onScanStateChanged(state: BleScanner.ScanState) {
        // Handled by Flow collection
    }
    
    override fun onStatsUpdated(stats: BleScanner.ScanStats) {
        // Handled by Flow collection
    }
    
    @Suppress("DEPRECATION")
    private fun vibrate(durationMs: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(durationMs)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scanner.stopScan()
    }
}
