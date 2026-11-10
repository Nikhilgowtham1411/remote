# Smart TV Remote - Android Application
### Mobile-Only | Wi-Fi Only | Google TV Compatible (Target: Lumio Vision 7 2026)

---

## 1. Project Overview

**Smart TV Remote** is a native Android application built with **Jetpack Compose**, **Material 3**, and **MVVM Architecture** that transforms an Android smartphone into a complete Smart TV control centre for Google TVs (specifically optimized for **Lumio Vision 7 (2026)** 55-inch Google TV).

### Core Architecture & Guarantees
- **Mobile-Only**: Runs exclusively on the user's Android smartphone.
- **Zero TV Installation**: No custom application, website, or server software is required on the TV. It communicates directly with Google TV's native system remote services.
- **Wi-Fi Only**: No Infrared (IR) Blaster or Bluetooth required; all control is achieved over local Wi-Fi.
- **Protocol**: Implements **Android TV Remote Protocol v2** over secure TLS on:
  - **Port 6467**: Secure pairing handshake (X.509 certificates and 4-digit PIN verification).
  - **Port 6466**: Authenticated remote control session (key injection, text typing, app launching, keep-alive ping/pong).
- **Google Cast SDK**: Native discovery and media casting for screen, video, photos, and music.
- **Wake-on-LAN**: UDP magic packet support to power on compatible Google TVs from standby mode over the local subnet.

---

## 2. Visual Design & Reference Screens

The UI directly recreates the 12 screens from the design reference with dark glassmorphic styling, neon blue/purple accents, OLED blacks, and accessible tactile controls:

| # | Screen | Description & Functionality |
|---|---|---|
| **01** | **Splash Screen** | App logo with glowing cyan/purple TV icon, tagline (*"Control • Browse • Cast • Play"*), neon arc, and loading spinner. Reconnects to saved TV. |
| **02** | **Welcome Screen** | Ambient TV graphic, key benefits (*Works over Wi-Fi*, *No TV app required*, *Easy & Big Buttons*, *For Everyone*), and primary gradient CTA (*Connect Your TV →*). |
| **03** | **Wi-Fi Check** | Glowing Wi-Fi badge with green checkmark, auto-detected SSID (e.g. `Home_WiFi_5G`), connection status verification. |
| **04** | **TV Discovery** | Real local network mDNS discovery (`_androidtvremote2._tcp` and `_googlecast._tcp`). Lists discovered TVs with model and IP. Includes *"Rescan"* and *"Enter IP Manually"* modal. |
| **05** | **Pairing** | Google TV Remote v2 pairing flow: TV displays 4-digit code, 4 large input boxes, *"Pair & Connect"* button with SHA-256 secret verification. |
| **06** | **Connection Success** | Vibrant glowing green checkmark, connected device card, *"Open Remote"* gradient CTA, and *"Change TV"* fallback. |
| **07** | **Main Remote** | **Primary control screen**: Top TV status header, red Power button (Wake-on-LAN capable), Mute button, vertical Volume controller (`+`, `VOL`, `-`), circular D-PAD (`Up`, `Down`, `Left`, `Right`, `OK`), vertical Channel controller (`+`, `CH`, `-`), Back/Home/Recent Apps navigation, popular app shortcuts (Netflix, YouTube, JioHotstar, Prime Video), and glowing voice search button. |
| **08** | **Playback Control** | Media details (*The Last of Us* poster, title, episode), dynamic progress scrubber (`12:34 / 56:10`), custom skip backward/forward buttons (e.g. `<< 10s` and `>> 10s`), play/pause, prev/stop/next, and quick link to skip duration settings. |
| **09** | **Skip Duration Settings** | Dedicated tabs for **Backward (↺)** and **Forward (↻)**, selectable presets (5s, 10s default, 15s, 30s, 60s), interactive **Custom duration selector** (`[-] 45s [+]`), persisted via DataStore. |
| **10** | **TV Apps** | Searchable app launcher with filters (*All Apps*, *Streaming*, *Installed*), 4-column grid of TV apps with branded badges (Netflix, YouTube, JioHotstar, Prime Video, Sony LIV, ZEE5, aha, MX Player, JioCinema, Disney+ Hotstar, Spotify, VLC). |
| **11** | **Cast to Your TV** | Google Cast SDK integration with 4 dedicated action cards: *Cast Screen*, *Cast Video* (active highlight), *Cast Photos*, and *Cast Music*. |
| **12** | **TV Keyboard** | On-screen search input with voice shortcut, full QWERTY virtual keyboard with Shift, backspace, numeric toggle, emoji, spacebar, and enter action key. |
| **13** | **Settings** | Connected TV details, reconnect/change TV, Connection Diagnostics dialog (Wi-Fi latency, ports 6466/6467, TLS handshake), Skip Duration, Haptic Feedback & Audio Clicks toggle, **Easy Mode / Large Buttons** accessibility toggle, and About section. |

---

## 3. Technology Stack & Dependencies

- **Language**: Kotlin 2.0.0
- **UI Framework**: Jetpack Compose (BOM `2024.08.00`), Material 3 (`1.2.1`), Material Icons Extended
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
- **Navigation**: Jetpack Navigation Compose (`2.8.0`)
- **Persistence**: Jetpack DataStore Preferences (`1.1.1`)
- **Protocol & Cryptography**:
  - TLSv1.2 client mutual authentication
  - BouncyCastle (`bcprov-jdk18on`, `bcpkix-jdk18on`) for on-device RSA 2048-bit keypair and X.509 self-signed certificate generation
  - SHA-256 digest hashing for pairing secret calculation
- **Discovery**: Android Network Service Discovery (`NsdManager`) for mDNS / DNS-SD
- **Google Cast**: `play-services-cast-framework:21.5.0`
- **Voice Recognition**: Android `SpeechRecognizer` API
- **Haptics**: Android `Vibrator` and `VibratorManager`

---

## 4. Project Structure

```
SmartTVRemote/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/smarttv/remote/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   └── RemotePreferences.kt       # DataStore persistence
│       │   │   ├── model/
│       │   │   │   ├── ConnectionStatus.kt        # Connection state machine
│       │   │   │   ├── MediaState.kt              # Media playback details
│       │   │   │   ├── RemoteCommand.kt           # Google TV KeyCodes
│       │   │   │   ├── SkipDurationConfig.kt      # Skip duration preferences
│       │   │   │   ├── TvApp.kt                   # TV apps & deep links
│       │   │   │   └── TvDevice.kt                # Discovered device model
│       │   │   └── protocol/
│       │   │       ├── CastManager.kt             # Google Cast media manager
│       │   │       ├── CastOptionsProvider.kt     # Cast framework receiver config
│       │   │       ├── CertUtils.kt               # RSA/X.509 certificate generator
│       │   │       ├── NsdDiscoveryManager.kt     # mDNS / DNS-SD discovery
│       │   │       ├── PairingManager.kt          # Google TV Port 6467 pairing flow
│       │   │       ├── RemoteSessionManager.kt    # Google TV Port 6466 TLS session
│       │   │       ├── SpeechManager.kt           # Voice recognition interpreter
│       │   │       ├── TlsSocketFactory.kt        # Mutual TLS socket factory
│       │   │       └── WakeOnLan.kt               # UDP Magic Packet for Power ON
│       │   ├── ui/
│       │   │   ├── components/
│       │   │   │   ├── AppShortcutButton.kt       # Branded app pills & grid cards
│       │   │   │   ├── BottomNavBar.kt            # 4-tab bottom navigation
│       │   │   │   ├── CircularDPad.kt            # Circular D-PAD controller
│       │   │   │   ├── GlassCard.kt               # Glassmorphic card surface
│       │   │   │   ├── GlowingMicButton.kt        # Assistant 4-color glowing mic
│       │   │   │   ├── NeonGradientButton.kt      # Gradient primary CTA button
│       │   │   │   ├── TvHeaderBar.kt             # Connected TV status pill & gear
│       │   │   │   └── VerticalSliderController.kt# Volume / Channel pill controllers
│       │   │   ├── navigation/
│       │   │   │   └── AppNavigation.kt           # NavHost & route coordinator
│       │   │   ├── screens/
│       │   │   │   ├── CastScreen.kt              # Screen 11: Cast to TV
│       │   │   │   ├── ConnectionSuccessScreen.kt # Screen 06: Pairing Success
│       │   │   │   ├── MainRemoteScreen.kt        # Screen 07: Main Remote
│       │   │   │   ├── PairingScreen.kt           # Screen 05: 4-digit PIN Pairing
│       │   │   │   ├── PlaybackControlScreen.kt   # Screen 08: Media Playback
│       │   │   │   ├── SetSkipDurationScreen.kt   # Screen 09: Skip Duration Settings
│       │   │   │   ├── SettingsScreen.kt          # Screen 13: App Settings
│       │   │   │   ├── SplashScreen.kt            # Screen 01: Neon Splash
│       │   │   │   ├── TvAppsScreen.kt            # Screen 10: App Launcher Grid
│       │   │   │   ├── TvDiscoveryScreen.kt       # Screen 04: Local TV Discovery
│       │   │   │   ├── TvKeyboardScreen.kt        # Screen 12: TV Keyboard
│       │   │   │   ├── WelcomeScreen.kt           # Screen 02: Welcome & Benefits
│       │   │   │   └── WifiCheckScreen.kt         # Screen 03: Wi-Fi Verification
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt                   # Neon Cyan, Blue, Purple, OLED Black
│       │   │   │   ├── Theme.kt                   # Material 3 dark theme setup
│       │   │   │   └── Type.kt                    # Typography system
│       │   │   └── viewmodel/
│       │   │       └── RemoteViewModel.kt         # Central UI & protocol state holder
│       └── res/
│           ├── drawable/
│           ├── mipmap-anydpi-v26/
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/
│               ├── backup_rules.xml
│               └── data_extraction_rules.xml
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
```

---

## 5. How to Build & Run

### In Android Studio
1. Open **Android Studio** (Hedgehog, Iguana, Jellyfish, or newer).
2. Choose **File → Open...** and select the folder `SmartTVRemote`.
3. Wait for Gradle Sync to complete.
4. Connect an Android device (or launch an Android Virtual Device / Emulator with API 26 to 34).
5. Click **Run ('app')** (or press `Shift + F10`).

### Via Command Line (Gradle)
```bash
cd SmartTVRemote

# On Windows:
gradlew.bat assembleDebug

# On macOS/Linux:
chmod +x gradlew
./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 6. Testing Instructions (Lumio Vision 7 / Google TV)

1. **Verify Wi-Fi Network**: Ensure both your Android phone and the Lumio Vision 7 TV are connected to the same Wi-Fi router (e.g. `Home_WiFi_5G`).
2. **Launch Application**: Open **Smart TV Remote**. The splash screen will transition to the Welcome screen.
3. **Verify Wi-Fi Status**: Tap *"Connect Your TV"*. Screen 03 will confirm your active Wi-Fi connection.
4. **Select TV**: On Screen 04 (*Choose Your TV*), your Lumio Vision 7 TV will appear automatically via mDNS discovery. You can also tap *"Enter IP Manually"* if mDNS is restricted on your router.
5. **Enter Pairing Code**: Tap on your TV. The TV screen will display a pairing code. Enter the 4 digits in Screen 05 and tap *"Pair & Connect"*.
6. **Remote Control**: Upon success, Screen 07 opens. You can now:
   - Navigate menus using the circular D-PAD (`Up`, `Down`, `Left`, `Right`, `OK`).
   - Adjust volume and channels using the vertical controllers.
   - Launch Netflix, YouTube, JioHotstar, and Prime Video instantly.
   - Use voice commands (*"Open YouTube"*, *"Volume Up"*, *"Home"*).
   - Switch to the **Media Playback** tab for seeking and pause/play.
   - Open **TV Keyboard** to type search queries directly onto the TV without needing an on-screen TV remote keyboard.
