package tel.packet.btrpascan

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Utility class for resolving Resolvable Private Addresses (RPAs) using
 * Identity Resolving Keys (IRKs) per the Bluetooth Core Specification.
 * 
 * Based on Bluetooth Core Spec Vol 3, Part H, Section 2.2.2
 * 
 * IMPORTANT: IRKs are NOT sniffable over the air. They are secret keys
 * exchanged during Bluetooth pairing over an encrypted channel and stored
 * locally on paired devices.
 * 
 * Users can obtain IRKs from:
 * - Linux: /var/lib/bluetooth/<adapter>/<device>/info
 * - macOS: Bluetooth plist files in /Library/Preferences
 * - Windows: Registry under HKLM\SYSTEM\CurrentControlSet\Services\BTHPORT\Parameters\Keys
 * - Android (rooted): /data/misc/bluedroid/bt_config.conf
 */
object IrkResolver {
    
    /**
     * Parse an IRK from a hex string (plain, colon-separated, or 0x-prefixed).
     * @param irkString The IRK as a hex string
     * @return 16 bytes or throws IllegalArgumentException
     */
    fun parseIrk(irkString: String): ByteArray {
        var s = irkString.trim()
        if (s.lowercase().startsWith("0x")) {
            s = s.substring(2)
        }
        s = s.replace(":", "").replace("-", "").replace(" ", "")
        
        if (s.length != 32) {
            throw IllegalArgumentException(
                "IRK must be exactly 16 bytes (32 hex chars), got ${s.length} hex chars"
            )
        }
        
        return try {
            s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("IRK contains invalid hex characters: $irkString")
        }
    }
    
    /**
     * Check if a MAC address string is valid format (XX:XX:XX:XX:XX:XX)
     */
    fun isValidMacAddress(address: String): Boolean {
        val macRegex = Regex("^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}$")
        return macRegex.matches(address)
    }
    
    /**
     * Check if a 6-byte address is a Resolvable Private Address.
     * RPA has top two bits of the most-significant byte set to 01.
     */
    fun isRpa(addressBytes: ByteArray): Boolean {
        return addressBytes.size == 6 && ((addressBytes[0].toInt() and 0xFF) shr 6) == 0b01
    }
    
    /**
     * Check if a MAC address string represents an RPA.
     */
    fun isRpa(address: String): Boolean {
        val bytes = macAddressToBytes(address) ?: return false
        return isRpa(bytes)
    }
    
    /**
     * Convert a MAC address string to bytes.
     */
    private fun macAddressToBytes(address: String): ByteArray? {
        val parts = address.replace("-", ":").split(":")
        if (parts.size != 6) return null
        
        return try {
            parts.map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * Bluetooth Core Spec ah() function (Vol 3, Part H, Section 2.2.2).
     * AES-128-ECB(IRK, padding || prand) -> return last 3 bytes.
     */
    private fun btAh(irk: ByteArray, prand: ByteArray): ByteArray {
        // 16 bytes: 13 zero-pad + 3-byte prand
        val plaintext = ByteArray(13) + prand
        
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val keySpec = SecretKeySpec(irk, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        
        val ciphertext = cipher.doFinal(plaintext)
        
        // Return last 3 bytes as hash
        return ciphertext.sliceArray(13..15)
    }
    
    /**
     * Resolve a MAC address string against an IRK.
     * 
     * MAC format: AA:BB:CC:DD:EE:FF
     * prand = first 3 octets (AA:BB:CC), hash = last 3 octets (DD:EE:FF).
     * Returns true if ah(IRK, prand) == hash.
     */
    fun resolveRpa(irk: ByteArray, address: String): Boolean {
        val addressBytes = macAddressToBytes(address) ?: return false
        
        if (!isRpa(addressBytes)) {
            return false
        }
        
        val prand = addressBytes.sliceArray(0..2)
        val expectedHash = addressBytes.sliceArray(3..5)
        
        val computedHash = btAh(irk, prand)
        
        return computedHash.contentEquals(expectedHash)
    }
    
    /**
     * Validate an IRK string without throwing.
     */
    fun validateIrk(irkString: String): String? {
        return try {
            parseIrk(irkString)
            null // Valid
        } catch (e: IllegalArgumentException) {
            e.message
        }
    }
    
    /**
     * Get help text explaining what IRKs are and how to obtain them.
     */
    fun getIrkHelpText(): String {
        return """
            |WHAT IS AN IRK?
            |
            |An Identity Resolving Key (IRK) is a 16-byte secret key used in Bluetooth Low Energy to identify devices that use Resolvable Private Addresses (RPAs).
            |
            |RPAs are randomized MAC addresses that change periodically to prevent tracking. Only devices that have the IRK can identify the real device behind the changing address.
            |
            |HOW TO OBTAIN AN IRK:
            |
            |IRKs are exchanged during Bluetooth pairing and stored locally. You can extract them from:
            |
            |• Linux:
            |  /var/lib/bluetooth/<adapter>/<device>/info
            |  Look for "IdentityResolvingKey" section
            |
            |• macOS:
            |  /Library/Preferences/com.apple.Bluetooth.plist
            |  Requires root access
            |
            |• Windows:
            |  Registry: HKLM\SYSTEM\CurrentControlSet\Services\BTHPORT\Parameters\Keys\<adapter>\<device>
            |  Look for "IRK" value
            |
            |• Android (requires root):
            |  /data/misc/bluedroid/bt_config.conf
            |  Look for "LE_LOCAL_KEY_IRK"
            |
            |IMPORTANT: IRKs cannot be sniffed from the air. They are exchanged over an encrypted channel during pairing.
        """.trimMargin()
    }
}
