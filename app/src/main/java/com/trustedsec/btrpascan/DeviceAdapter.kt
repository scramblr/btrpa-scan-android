package com.trustedsec.btrpascan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for displaying BLE devices.
 */
class DeviceAdapter(
    private val onItemClick: (BleDeviceInfo) -> Unit
) : ListAdapter<BleDeviceInfo, DeviceAdapter.ViewHolder>(DeviceDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return ViewHolder(view, onItemClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        itemView: View,
        private val onItemClick: (BleDeviceInfo) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val addressText: TextView = itemView.findViewById(R.id.textAddress)
        private val nameText: TextView = itemView.findViewById(R.id.textName)
        private val rssiText: TextView = itemView.findViewById(R.id.textRssi)
        private val distanceText: TextView = itemView.findViewById(R.id.textDistance)
        private val seenText: TextView = itemView.findViewById(R.id.textSeen)
        private val lastSeenText: TextView = itemView.findViewById(R.id.textLastSeen)
        private val irkBadge: TextView = itemView.findViewById(R.id.badgeIrk)
        
        fun bind(device: BleDeviceInfo) {
            addressText.text = device.address
            nameText.text = device.displayName
            
            // RSSI with optional average
            val rssiDisplay = if (device.avgRssi != null) {
                "${device.rssi} dBm (avg: ${device.avgRssi})"
            } else {
                "${device.rssi} dBm"
            }
            rssiText.text = rssiDisplay
            
            // Color code RSSI
            val rssiColor = when {
                device.rssi >= -50 -> R.color.rssi_strong
                device.rssi >= -70 -> R.color.rssi_medium
                else -> R.color.rssi_weak
            }
            rssiText.setTextColor(ContextCompat.getColor(itemView.context, rssiColor))
            
            // Distance
            distanceText.text = DistanceEstimator.formatDistance(device.estimatedDistance)
            
            // Times seen
            seenText.text = "${device.timesSeen}x"
            
            // Last seen
            lastSeenText.text = device.lastSeenFormatted
            
            // IRK resolution badge
            when (device.irkResolved) {
                true -> {
                    irkBadge.visibility = View.VISIBLE
                    irkBadge.text = "IRK ✓"
                    irkBadge.setBackgroundResource(R.drawable.badge_irk_match)
                }
                false -> {
                    irkBadge.visibility = View.VISIBLE
                    irkBadge.text = "RPA"
                    irkBadge.setBackgroundResource(R.drawable.badge_rpa)
                }
                null -> {
                    irkBadge.visibility = View.GONE
                }
            }
            
            itemView.setOnClickListener { onItemClick(device) }
        }
    }
    
    class DeviceDiffCallback : DiffUtil.ItemCallback<BleDeviceInfo>() {
        override fun areItemsTheSame(oldItem: BleDeviceInfo, newItem: BleDeviceInfo): Boolean {
            return oldItem.address == newItem.address
        }
        
        override fun areContentsTheSame(oldItem: BleDeviceInfo, newItem: BleDeviceInfo): Boolean {
            return oldItem == newItem
        }
    }
}
