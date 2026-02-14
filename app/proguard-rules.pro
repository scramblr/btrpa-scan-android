# Add project specific ProGuard rules here.

# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep BleDeviceInfo for JSON serialization
-keep class tel.packet.btrpascan.BleDeviceInfo { *; }
