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

If you're itching to just GIT GUD grab the APK here. I signed it with my own hooves: [https://github.com/scramblr/btrpa-scan-android/releases](https://github.com/scramblr/btrpa-scan-android/releases)
I'll be releasing it for the official Google Play Store once they decide my 5 year old company is real or whatever they're doing. iOS users: hold ur breath, a version is coming! keeeeeeeeep holding!! 
---

## What's In This Project?

Death & Depair. Haha, no wait, that's my life.. For this project I included a lot more files than this, but these are the most important:

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

#### The rest of the files are mostly just artifacts from Android Studio building the app on my box, I doubt it'll mess with your setup but I'll be cleaning this all up later. ####
#### _I Figured I'd rather release this now so people can play and hopefully contribute to make this more badass, and then we'll clean it up and make it all sparkly-like laterz ;)_ ####
---

## Step-by-Step Build Instructions

### Prerequisites

- **Android Studio Panda** (2025.3.1 Patch 1) or newer
- **A Physical Android Phone** - BLE scanning does NOT work in the emulator. I am using a Samsung and Pixel to do primary testing. If you get it working on something else, please lemme know!
- **USB cable** to connect your phone to your computer (the data kind, power charge-only cables should be illegal)
- **Developer Mode enabled** on your phone (instructions below if u dont already know)

---

### Step 1: Extract the Zip File or Clone the Repo

- Download `btrpa-scan-android.zip` or clone this Repo.
- By default it should be called `btrpa-scan-android`

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

**This may take 5-10 minutes** the first time as it downloads dependencies. There's a pretty decent amount of them, so it's going to be a little bit.

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

### Step 5: Enable Developer Mode on Your Android Phone if it's not already Enabled. No root required.

You need to enable Developer Options to install apps from Android Studio, as well as turn off WiFi scan throttling and other crap that interferes with your Android being an awesome scanner for this tool, as well as [wigle.net](https://wigle.net)'s scanner:

1. On your phone, go to **Settings**
2. Scroll down and tap **About Phone** (might be under "System" on some phones)
3. Find **"Build Number"**
4. **Tap "Build Number" 7 times rapidly**
5. You'll see a message: "You are now a developer!"
6. If that doesn't work maybe try tapping 1294 times just to be sure.

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
5. You can also run "adb devices" from windows cmd and it should show your device if you have your paths set right and whatever..

**If your phone doesn't appear:**
- Try a different USB cable (some cables are charge-only)
- Try a different USB port
- Make sure USB Debugging is enabled (Step 5)
- On Windows, you may need to install USB drivers for your phone because it's not 2026 or anything
- You might be trying to plug the USB cable in to a banana. Bananas do not currently scan BLE smoothly, but progress is being made.

---

### Step 7: Build and Run the App (or just download and run it from the Releases section on the right side of Github page. up2u)

1. In Android Studio's top toolbar, make sure your phone is selected in the device dropdown
2. Click the **green Play button ▶** (or press `Shift+F10`)
3. Android Studio will:
   - Compile the app (you'll see progress at the bottom)
   - Install it on your phone
   - Launch the app automatically

**First build may take 2-5 minutes.** Subsequent builds are faster.

Alternatively, grab the APK that I signed with my own hooves at [https://github.com/scramblr/btrpa-scan-android/releases](https://github.com/scramblr/btrpa-scan-android/releases) 

---

### Step 8: Grant Permissions on Your Phone

When the app launches for the first time:

1. Tap the **Bluetooth scan button** (floating button in bottom-right corner)
2. The app will ask for permissions - **grant all of them**:
   - Bluetooth permission → Allow
   - Location permission → Allow (required for BLE on older Android versions)
   - Nearby devices permission → Allow
3. If Bluetooth is off, the app will ask you to turn it on → Tap "Allow"

### Step 9: Marvel.
1. ...At the lack of requesting permissions for your GPS coordinates and other bullshit that tons of these BLE scanners insist they need cuz I'm not a dickhead.
2. This app doesnt phone home, play games, or take names.. it just lets u hunt people very effectively to a degree of inches. I mean, hunt devices. Not People. ;)

---

## How to Use This Thing

### Discover All Bluetooth BLE Devices & Hack All The Things
1. Select the **"Discover All"** chip at the top
2. Tap the **scan button** (Bluetooth icon in bottom-right)
3. Nearby BLE devices will appear in a list
4. Tap any device to see detailed information

### Search for a Specific Device - Useful for tracking down stolen equipment you know the MAC address too (as long as it doesn't rotate, see below..)
1. Select the **"Target MAC"** chip
2. Enter a MAC address (format: `AA:BB:CC:DD:EE:FF`)
3. Start scanning - only that device will show up when found
4. Yes I know, I need to make a loop function to continuously scan. Or you could, and then submit the pull! That'd be rad!

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

## Understanding WTF You're Even Doing With These RPAs and IRKs

### What is an RPA?

A **Resolvable Private Address (RPA)** is a randomized Bluetooth MAC address that changes periodically (typically every 15 minutes). Devices use RPAs to prevent tracking by third parties. Except there's a problem: Feds wanted a way to track you, and so there's a way to track you.

The app automatically detects RPAs and marks them with a purple **"RPA"** badge.

### What is an IRK?

An **Identity Resolving Key (IRK)** is a 16-byte secret key that allows you to identify a device even when it's using randomized RPA addresses. This is how the feds and now anyone can track you, even if you deploy this "Security Feature" AKA "Security Leaker/Bugdoor"

**Important:** IRKs are NOT transmitted over the air frequently like how WEP/WiFi works, so it can't be easily sniffed. **IRKs are exchanged during Bluetooth pairing over an encrypted channel that nobody could ever possibly intercept and break. Don't look in to this, kids!**

### How to Obtain an IRK Without Sniffing via the Bug Doored BLE Protocol

IRKs are super convienently stored locally on all paired devices! Including the ones you throw in the trash. You can extract them from:

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
| Yes I will be adding more phones that are older and older APIs | relax |
| Phone Features | Bluetooth Low Energy (BLE) |
| Actually Being Able To Use This | Physical Device Required (not emulator, i dont think? try it!) |

---

## TO-DO LIST
- Implement pairing options for found devices
- Implement other fun BLE/Bluetooth fuckery that's in other tools
- Optionally give the ability to plot these on a map or whatever
- Maybe just integrate with [WIGLE.NET](https://wigle.net), or if [@wiglenet](https://wigle.net) sees this and thinks it's interesting, they might just rip the useful stuff out of this and integrate it into their already awesome tool!
- Attempt to create method of extracting the IRKs from the air (dont hold ur breath for now..)

## Credits

- **Original Tool**: [btrpa-scan](https://github.com/hackingdave/btrpa-scan)
- **Original Author**: David Kennedy [@HackingDave](https://x.com/@HackingDave)
- **Company**: [TrustedSec](https://trustedsec.com)
- **This Android Version Created By**: Not Dan [@notdan](https://x.com/notdan) AKA scramblr sometimes, and other names.
- **Company**: [PACKET.TEL](https://packet.tel)
