package com.trustedsec.btrpascan

import kotlin.math.pow

/**
 * Utility for estimating distance from RSSI and TX Power using
 * the log-distance path loss model.
 */
object DistanceEstimator {
    
    /**
     * Environment presets with their path loss exponents.
     */
    enum class Environment(val pathLossExponent: Double, val displayName: String) {
        FREE_SPACE(2.0, "Free Space"),
        OUTDOOR(2.2, "Outdoor"),
        INDOOR(3.0, "Indoor")
    }
    
    /**
     * Estimate distance in meters using the log-distance path loss model.
     * 
     * @param rssi Current RSSI value in dBm
     * @param txPower Transmitted power at 1 meter in dBm (from advertisement)
     * @param environment Environment preset for path loss calculation
     * @return Estimated distance in meters, or null if calculation not possible
     */
    fun estimateDistance(
        rssi: Int,
        txPower: Int?,
        environment: Environment = Environment.FREE_SPACE
    ): Double? {
        if (txPower == null || rssi == 0) {
            return null
        }
        
        // Distance = 10 ^ ((TxPower - RSSI) / (10 * n))
        // where n is the path loss exponent
        return 10.0.pow((txPower - rssi).toDouble() / (10 * environment.pathLossExponent))
    }
    
    /**
     * Format distance for display.
     */
    fun formatDistance(distance: Double?): String {
        return when {
            distance == null -> "N/A"
            distance < 1.0 -> "~${String.format("%.1f", distance * 100)}cm"
            distance < 10.0 -> "~${String.format("%.1f", distance)}m"
            else -> "~${String.format("%.0f", distance)}m"
        }
    }
}
