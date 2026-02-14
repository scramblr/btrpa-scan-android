# BTRPA-SCAN-ANDROID - A Bluetooth BLE Scanner that can find devices that change MAC addresses 😈

#### This is an Android Port of [@HackingDave's](https://x.com/HackingDave) Bluetooth Low Energy (BLE) Scanner with Resolvable Private Address (RPA) Resolution using Identity Resolving Keys (IRKs) in order to track those silly devices that think changing a MAC address will save them from being tracked. ####
#### The original version can be found here: [https://github.com/HackingDave/btrpa-scan](https://github.com/HackingDave/btrpa-scan) ####
---

## Hey! Here's The Deal, From the GROUND TO THE SKY! ##

This app is an **Android version** of the [btrpa-scan](https://github.com/hackingdave/btrpa-scan) Python tool. It SHOULD (_once all bugs are found and worked out_):

1. **Scan for Bluetooth devices** - Finds all BLE devices broadcasting near you
2. **Search for specific devices** - Hunt for a device by its MAC address
3. **Resolve hidden addresses** - Some Bluetooth devices use "Resolvable Private Addresses" (RPAs) that change randomly. If you have the device's secret key (called an IRK), this app can identify those devices
4. **Have occasional issues** - This is the first release and it's in alpha right now, but I'm happy to release early and let you guys make things better. _Full Disclosure: This relies heavily on vibrator code from Claude (Opus 4.5)._

---

## What's In This Project?

When you extract the zip file, you'll see this folder structure:

```
btrpa-scan-android/
├── app/
│   ├── src/main/
│   │   ├── java/tel/packet/btrpascan/        <-- The actual code
│   │   │   ├── MainActivity.kt               <-- Main screen
│   │   │   ├── BleScanner.kt                 <-- Bluetooth scanning logic
│   │   │   ├── IrkResolver.kt                <-- Cryptographic key resolution
│   │   │   ├── BleDeviceInfo.kt              <-- Device data structure
│   │   │   ├── DeviceAdapter.kt              <-- List display
│   │   │   ├── DistanceEstimator.kt          <-- Distance calculation
│   │   │   └── ExportManager.kt              <-- Save results to file
│   │   ├── res/                              <-- UI layouts, colors, icons
│   │   └── AndroidManifest.xml               <-- App permissions & config
│   └── build.gradle.kts                      <-- App build settings
├── build.gradle.kts                          <-- Project build settings
├── settings.gradle.kts                       <-- Project structure
├── gradle.properties                         <-- Build options
├── gradlew.bat                               <-- Build script (Windows)
├── gradlew                                   <-- Build script (Mac/Linux)
└── README.md                                 <-- This file
```

---

## Step-by-Step Build Instructions

### Prerequisites

- **Android Studio Panda** (2025.3.1 Patch 1) or newer
- **A physical Android phone** - BLE scanning does NOT work in the emulator
- **USB cable** to connect your phone to your computer
- **Developer Mode enabled** on your phone (instructions below)

---

### Step 1: Extract the Zip File

1. Download `btrpa-scan-android.zip`
2. Right-click the zip file
3. Select "Extract All..." (Windows) or double-click (Mac)
4. Choose a location you'll remember (like your Desktop or Documents folder)
5. You should now have a folder called `btrpa-scan-android`

---

### Step 2: Open the Project in Android Studio

1. **Open Android Studio**
2. On the Welcome screen, click **"Open"** (NOT "New Project")
   - If Android Studio opens an existing project, go to **File → Open**
3. Navigate to where you extracted the zip
4. Select the **`btrpa-scan-android`** folder (the main folder, not a subfolder)
5. Click **"OK"** or **"Open"**

---

### Step 3: Wait for Gradle Sync

When the project opens, Android Studio will automatically start "syncing" the project. You'll see:

- A progress bar at the bottom of the screen
- Text saying "Gradle sync in progress..." or similar

**This may take 2-10 minutes** the first time as it downloads dependencies.

**If you see errors:**
- Look for a blue "Try Again" link and click it
- Or go to **File → Sync Project with Gradle Files**

**Common Issues:**
- "SDK not found" → Go to **File → Project Structure → SDK Location** and set your Android SDK path
- "JDK version" errors → Android Studio Panda should handle this automatically

---

### Step 4: Check Project Settings

1. Go to **File → Project Structure** (or press `Ctrl+Alt+Shift+S` on Windows)
2. Click **"Project"** in the left sidebar
3. Verify:
   - **Gradle Version**: Should be 8.6 or higher
   - **Android Gradle Plugin Version**: Should be 8.4.0 or higher
4. Click **"Modules"** in the left sidebar
5. Click on **"app"**
6. Verify:
   - **Compile SDK Version**: 34
   - **Min SDK Version**: 26
   - **Target SDK Version**: 34
7. Click **"OK"** to close

---

### Step 5: Enable Developer Mode on Your Android Phone

You need to enable Developer Options to install apps from Android Studio:

1. On your phone, go to **Settings**
2. Scroll down and tap **About Phone** (might be under "System" on some phones)
3. Find **"Build Number"**
4. **Tap "Build Number" 7 times rapidly**
5. You'll see a message: "You are now a developer!"

Now enable USB Debugging:

1. Go back to **Settings**
2. Tap **System** (if present)
3. Tap **Developer Options** (new option that appeared)
4. Scroll down and enable **USB Debugging**
5. Tap "OK" on the confirmation dialog

---

### Step 6: Connect Your Phone

1. Connect your Android phone to your computer with a USB cable
2. On your phone, you may see a popup: "Allow USB debugging?"
   - Check "Always allow from this computer"
   - Tap "Allow"
3. In Android Studio, look at the top toolbar
4. You should see a dropdown that now shows your phone's name (like "Pixel 7" or "Samsung SM-G998")

**If your phone doesn't appear:**
- Try a different USB cable (some cables are charge-only)
- Try a different USB port
- Make sure USB Debugging is enabled (Step 5)
- On Windows, you may need to install USB drivers for your phone

---

### Step 7: Build and Run the App

1. In Android Studio's top toolbar, make sure your phone is selected in the device dropdown
2. Click the **green Play button ▶** (or press `Shift+F10`)
3. Android Studio will:
   - Compile the app (you'll see progress at the bottom)
   - Install it on your phone
   - Launch the app automatically

**First build may take 2-5 minutes.** Subsequent builds are faster.

---

### Step 8: Grant Permissions on Your Phone

When the app launches for the first time:

1. Tap the **Bluetooth scan button** (floating button in bottom-right corner)
2. The app will ask for permissions - **grant all of them**:
   - Bluetooth permission → Allow
   - Location permission → Allow (required for BLE on older Android versions)
   - Nearby devices permission → Allow
3. If Bluetooth is off, the app will ask you to turn it on → Tap "Allow"

---

## How to Use the App

### Discover All Devices
1. Select the **"Discover All"** chip at the top
2. Tap the **scan button** (Bluetooth icon in bottom-right)
3. Nearby BLE devices will appear in a list
4. Tap any device to see detailed information

### Search for a Specific Device
1. Select the **"Target MAC"** chip
2. Enter a MAC address (format: `AA:BB:CC:DD:EE:FF`)
3. Start scanning - only that device will show up when found

### IRK Resolution Mode
1. Select the **"IRK Resolve"** chip
2. Enter a 16-byte key in hex format (32 characters)
3. Start scanning - devices that match the key will be highlighted

### Settings (tap the gear icon)
- **Active Scanning**: Gets more device info but uses more battery
- **Min RSSI**: Filter out weak/distant signals (try -70)
- **RSSI Window**: Average multiple readings for stability
- **Proximity Alert**: Vibrate when a device gets close
- **Timeout**: How long to scan (in seconds)
- **Environment**: Affects distance calculation accuracy

### Export Results
- Tap the menu (3 dots) → Export as CSV or JSON
- Share the file via email, cloud storage, etc.

---

## Understanding RPAs and IRKs

### What is an RPA?

A **Resolvable Private Address (RPA)** is a randomized Bluetooth MAC address that changes periodically (typically every 15 minutes). Devices use RPAs to prevent tracking by third parties.

The app automatically detects RPAs and marks them with a purple **"RPA"** badge.

### What is an IRK?

An **Identity Resolving Key (IRK)** is a 16-byte secret key that allows you to identify a device even when it's using randomized RPA addresses.

**Important:** IRKs are NOT transmitted over the air and cannot be "sniffed." They are exchanged during Bluetooth pairing over an encrypted channel.

### How to Obtain an IRK

IRKs are stored locally on paired devices. You can extract them from:

| Platform | Location |
|----------|----------|
| **Linux** | `/var/lib/bluetooth/<adapter>/<device>/info` - Look for `IdentityResolvingKey` |
| **macOS** | `/Library/Preferences/com.apple.Bluetooth.plist` (requires root) |
| **Windows** | Registry: `HKLM\SYSTEM\CurrentControlSet\Services\BTHPORT\Parameters\Keys\<adapter>\<device>` |
| **Android** (rooted) | `/data/misc/bluedroid/bt_config.conf` - Look for `LE_LOCAL_KEY_IRK` |

---

## Troubleshooting

### "Gradle sync failed"
- Make sure you have internet connection
- Go to **File → Invalidate Caches and Restart**
- Try **File → Sync Project with Gradle Files**

### "SDK not installed"
- Go to **Tools → SDK Manager**
- Make sure "Android 14.0 (API 34)" is checked and installed

### "No devices found" when scanning
- Make sure Bluetooth is ON
- Make sure you granted all permissions
- Make sure there are actually BLE devices nearby (phones, fitness trackers, etc.)
- Try moving closer to known Bluetooth devices

### App crashes immediately
- Check **Logcat** in Android Studio (bottom panel) for error messages
- Make sure you're running on a physical device, not an emulator

### Phone not detected by Android Studio
- Try a different USB cable
- Install your phone manufacturer's USB drivers
- Restart Android Studio
- Restart your phone

### Gradle build fails with version errors
- Go to **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
- Set **Gradle JDK** to "JetBrains Runtime" or "Embedded JDK"
- Click OK and try **File → Sync Project with Gradle Files**

---

## Building Release APKs

### Using Android Studio (Recommended)

1. **Build → Generate Signed Bundle / APK**
2. Select **APK** → Next
3. Create a keystore (first time) or select existing
4. Select **release**, check V1 and V2 signatures
5. Click **Create**

### Using Command Line

```powershell
# Windows PowerShell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleRelease
```

```bash
# Mac/Linux
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

---

## Requirements Summary

| Requirement | Value |
|------------|-------|
| Android Studio | Panda 2025.3.1 or newer |
| Minimum Android Version | 8.0 (API 26) |
| Target Android Version | 14 (API 34) |
| Phone Features | Bluetooth Low Energy (BLE) |
| Testing | Physical device required (not emulator) |

---

## Credits

- **Original Tool**: [btrpa-scan](https://github.com/hackingdave/btrpa-scan)
- **Original Author**: David Kennedy [@HackingDave](https://x.com/@HackingDave)
- **Company**: [TrustedSec](https://trustedsec.com)
- **Android Port**: Not Dan [@notdan](https://x.com/notdan)
- **Company**: PACKET.TEL
