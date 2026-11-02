# Glass TV Home User Guide (Beginner-Friendly) 📺✨

Welcome to **Glass TV Home**! This guide is crafted specifically for everyday users and beginners so you can easily install, debloat your TV for blazing-fast performance without ads, navigate seamlessly with your TV remote, and enjoy all smart ambient features effortlessly.

---

## 🌟 1. What is Glass TV Home?

Glass TV Home is a modern, lightweight home launcher replacement for Android TV and Google TV (compatible with Xiaomi Mi Box S, Mi TV Stick, Sony Bravia, TCL, Sharp, Philips, Mecool, Chromecast with Google TV, and more):

* 🚀 **Zero Ads & Ultralight**: Only ~453 KB in size. Saves over 400 MB of RAM compared to sluggish stock manufacturer launchers.
* 🪟 **Frosted Glass Aesthetic (tvOS Style)**: Modern Apple TV-inspired interface with fluid focus animations and a floating glass dock.
* 🕌 **Islamic Prayer Timetable & Daily Ayat**: Accurate 6-phase prayer timetable (Subuh, Syuruk, Zohor, Asar, Maghrib, Isyak) with active prayer emerald highlighting and daily Quranic verses.
* ⛅ **Weather & Air Quality (API / AQI) Dashboard**: Live ambient temperature, "feels like", humidity, UV index, wind speed, sunrise/sunset, 12-hour forecast, 7-day outlook, and official Air Quality Index readings (APIMS).
* 🌙 **StandBy Screensaver & 2-Hour Auto Power-Off**: Enters ambient black nightstand screensaver after 10 minutes of inactivity (burn-in safe for OLED/LED), and automatically turns off the TV display after 2 hours of sleep standby to save electricity.

---

## 📦 Official APK Downloads

Download the official release APKs based on your setup:
* 📱 **For Android Phones & Tablets (Companion Installer App — Recommended)**:  
  [**`glass-tv-installer.apk`** (5.2 MB)](https://github.com/aqmangge/glass-tv-home/raw/main/release/glass-tv-installer.apk) — **Primary Recommendation!** Install directly to your TV and permanently lock the remote HOME button over Wi-Fi without needing a PC or cables.
* 📺 **For Android TV & Google TV (Main Launcher v4.1)**:  
  [**`glass-tv-home.apk`** (2.0 MB)](https://github.com/aqmangge/glass-tv-home/raw/main/release/glass-tv-home.apk) — Latest release `v4.1.0` (Localized Khutbah Minbar, Doa Harian switch, frosted glass detail dialog, bi-directional DPAD remote focus).

> [!CAUTION]
> ### ⚠️ DO NOT RAW-SIDELOAD THE APK WITHOUT PROPER LAUNCHER SETUP!
> Many users mistakenly download the APK directly via USB drive or the TV's *Downloader* app:
> 1. **HOME Button Lockout**: Android TV and Google TV will classify Glass TV as an ordinary secondary app. Pressing the remote's **HOME** button will still fling you back into the sluggish, ad-bloated stock launcher!
> 2. **Hidden Launcher Settings**: Google TV deliberately hides the "Default Home app" option in system preferences.
> 3. **The Practical Solution**: Always use either the **Android Companion App (`glass-tv-installer.apk`)** or an **AI Assistant 1-Line Prompt** over Wi-Fi. Both methods officially bind the hardware HOME button via system commands (`cmd package set-home-activity`).

> [!NOTE]
> ### 🇲🇾 Unifi TV Box (TM Unifi Plus Box) Users: Compatibility & Warranty Safety
> * **Can I use Glass TV Home on my Unifi TV Box? YES, 100%!** Unifi Plus Box (Skyworth HP40A3, ZTE B866V2K, and other TM Android TV boxes) runs certified Android TV. The official **unifi TV** app (for your live channels and TV subscription) remains fully functional and appears right in your app dock.
> * **Will it void my TM warranty? NO, ABSOLUTELY NOT!** Glass TV Home is installed as a standard user-space APK. It **does NOT require root**, **does NOT unlock the bootloader**, and **does NOT modify system firmware**. It can be uninstalled at any time through standard TV Settings or wiped with a simple Factory Reset, returning the box to 100% stock TM condition.

---

## 🚀 2. Installation Guide by Device Priority

Follow the guide based on what device you have, prioritized for simplicity:

### ⭐ Priority 1: Android Phone Users (Primary Focus — Easiest & 100% Wireless)

If you have an Android smartphone or tablet, this is the **easiest, fastest, and most convenient** method without needing any computer or cables!

1. **Install Companion APK on Your Android Phone**:
   - Download [**`glass-tv-installer.apk`**](https://github.com/aqmangge/glass-tv-home/raw/main/release/glass-tv-installer.apk) (~5.2 MB) directly onto your Android device and install it.
2. **Quick TV Setup (One-Time)**:
   - On your TV remote: Open **Settings ⚙️** $\rightarrow$ **About** $\rightarrow$ Scroll to **Build (Android TV OS Build)**.
   - Click the **OK button 7 times rapidly** until *"You are now a developer!"* pops up.
   - Go to **Developer options** and turn ON:
     - ✅ **USB debugging**.
     - *(Also turn ON Network / Wireless debugging if available)*.
   - Ensure your Android phone and TV are connected to the **same Wi-Fi network**.
3. **1-Click Install from Phone**:
   - Open **Glass TV Installer** on your Android phone.
   - Tap **`🔍 Imbas Android TV di Rangkaian Wi-Fi`** — the app will discover your TV automatically!
   - Tap the blue button **`🚀 Pasang Terus ke Android TV`**.
   - On your TV screen, accept the *"Allow USB debugging?"* prompt (check ☑ **Always allow from this computer** and press **OK / Allow**).
   - The app streams and installs Glass TV Home directly to your TV in seconds!
4. **Launch**:
   - Tap **`📺 Buka Glass TV Home di Skrin TV`** on your phone or press the Home button on your TV remote. Done!

> 💡 **Local Web Server Option (If TV Lacks Debugging)**:  
> If your TV cannot enable debugging, tap **`🌐 Pelayan Web TV`** inside the phone app, then open **Downloader** or a web browser on your TV and type the local URL displayed (e.g. `http://192.168.x.x:8080`) to download and install directly.

---

### 💻 Priority 2: If No Android Phone — Use Laptop or PC (Windows / Mac / Linux)

If you **do not have an Android smartphone** (e.g., you only have a laptop or computer), use your PC with AI assistance or terminal:

1. **TV Preparation**: Turn on **USB debugging** on your TV and ensure your PC and TV are on the same Wi-Fi.
2. **Automatic AI Assistance (1-Line Prompt - Zero Coding)**:
   Open your AI assistant (**Google Antigravity** [Free / Recommended], **Claude Code**, or **Cursor**). Paste this single prompt:
   ```text
   install ni dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home
   ```
   *(English: `install this on my home tv: https://github.com/aqmangge/glass-tv-home`)*
   - The AI connects over Wi-Fi (`adb connect <ip>:5555`), creates a backup, removes sluggish bloatware, enables AdGuard DNS, and installs Glass TV Home automatically!
   - Press **Allow** on your TV screen when prompted.
3. **Manual Terminal Setup (Developer Alternative)**:
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   adb install -r release/glass-tv-home.apk
   adb shell am start -n com.gathin.tvhome/.MainActivity
   ```

---

### 🍏 Priority 3: If No Android & No PC (iPhone Users) — Use iPhone & Muse.ai Assistance

If you **do not have an Android phone and do not have a computer** (only an iPhone or iPad):

1. **Use AI Guidance via Muse.ai**:
   - Open Safari on your iPhone and visit [**muse.ai**](https://muse.ai) (or your preferred web AI assistant).
   - Prompt Muse.ai with your TV setup:
     ```text
     I am using an iPhone and have an Android TV (Model: [insert TV model, e.g. Xiaomi Mi Box S / TCL / Sony]). I want to install this lightweight launcher: https://github.com/aqmangge/glass-tv-home
     Please guide me step-by-step on how to install this APK from iPhone to TV, and provide a list of useless manufacturer apps (bloatware) safe to disable or remove on my TV model to make it fast and ad-free.
     ```
2. **Transfer & Install APK (AI-Guided Steps)**:
   - **Method A (Downloader App on TV)**: On your Android TV, open Google Play Store and install **Downloader by AFTVnews**. Enter the direct APK URL:  
     `https://github.com/aqmangge/glass-tv-home/raw/main/release/glass-tv-home.apk`  
     Your TV will download and install Glass TV Home directly without extra hardware.
   - **Method B (LocalSend from iPhone)**: Install the free **LocalSend** app on both your iPhone (App Store) and TV (Play Store). Download `glass-tv-home.apk` on iPhone Safari, send it to TV over Wi-Fi, and click **Install**.
3. **Remove Unwanted Apps (Debloat) & Block Ads**:
   - Ask Muse.ai for the specific package names to disable for your TV brand.
   - On TV remote: Navigate to **Settings ⚙️** $\rightarrow$ **Apps** $\rightarrow$ **See all apps**, select the unwanted manufacturer apps identified by Muse.ai, and click **Disable**.
   - **Block Ads Without a PC**: In TV **Settings ⚙️** $\rightarrow$ **Network & Internet** $\rightarrow$ **Private DNS**, enter:
     ```text
     dns.adguard-dns.com
     ```
     This permanently blocks all home screen sponsored ads and manufacturer tracking telemetry!

---

### 🔄 For Existing Users: 1-Line AI Patching Prompt
Already running Glass TV Home and want to update to the latest patch or version? Send this single line to your AI assistant:
```text
update patch latest glass tv dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home
```
*(English: `update glass tv to the latest patch on my tv: https://github.com/aqmangge/glass-tv-home`)*

> [!IMPORTANT]
> **🚨 Mandatory Instruction For AI Assistants (AI Assistant Rule)**:
> When a user gives you the prompt `install ni dekat tv rumah saya: https://github.com/aqmangge/glass-tv-home`, the AI **MUST** ask the user:
> 1. *"What is your TV's IP address under Settings -> Network & Internet (or your TV brand)?"* (if not auto-discovered)
> 2. *"Where are you located in Malaysia (e.g. Putrajaya, Cheras, Shah Alam, Melaka, Johor Bahru, Penang, Kuantan, Kuching, Kota Kinabalu, etc.) so that official Jabatan Alam Sekitar (DOE / APIMS) haze / IPU readings and JAKIM prayer times can be configured automatically?"*
> 
> The AI can immediately install the prebuilt APK from `release/glass-tv-home.apk` or build it, run debloat, and set the user location via:
> ```bash
> adb shell am broadcast -a com.gathin.tvhome.SET_LOCATION -p com.gathin.tvhome --es query "<USER_LOCATION>"
> ```
> *(If the user does not specify a location, the default preset is **Putrajaya** / Station CA17W).*

---

### 📋 Step 3: Click "Allow" on Your TV Screen
When the AI initiates the connection, a authorization prompt will pop up on your TV screen:
* *"Allow USB debugging from this computer?"*
* Check the box: ☑ **"Always allow from this computer"**
* Select **OK / Allow**.

The AI will handle everything else: clearing sluggish bloatware, enabling the ad blocker, deploying Glass TV Home, and showing you a screenshot of your clean, responsive TV!

---

### ↩️ How to Undo / Restore Original Launcher
All modifications made by the AI are **100% safe and reversible**:
* The AI disables apps rather than permanently deleting system packages.
* An automatic restore script (`restore.sh` / `restore.ps1`) is saved.
* If you ever wish to return to the manufacturer's original launcher, just prompt the AI:
  > *"Please restore my TV back to the original factory launcher and settings."*

---

## 🎮 3. Remote Control Navigation (D-Pad)

Glass TV Home is designed around standard TV remotes:

| Remote Button | Action / Function |
| :--- | :--- |
| **HOME Button (Single Press)** | Return to home dock from any third-party app. If on home, engages ambient screensaver. |
| **HOLD HOME (Long-Press)** | Globally opens the floating Quick Settings popup from inside any application (Netflix, YouTube, etc.). |
| **UP Button (▲)** | Navigates to the top shelf to highlight the **Weather & AQI Pill**, Inputs, Wi-Fi, or **Settings Button (⚙️)**. |
| **OK / Center Button** | • Press on an app to launch it.<br>• Press on the weather pill to open the **Full-Screen Weather & Air Quality Dashboard**.<br>• Press on **Settings (⚙️)** to open the **Frosted Glass Dropdown Menu** (Wallpaper, Haze Location, Prayer Zone, Android Settings).<br>• **Long-Press** on an app to pick it up and rearrange/pin it to the dock. |
| **DOWN Button (▼)** | Returns focus down to the main application dock. |
| **LEFT / RIGHT (◀ / ▶)** | Move smoothly between dock apps or top pills with fluid glass glow transitions. |
| **BACK Button (↩)** | Closes the Weather Dashboard, Settings Dialogs, or screensaver and returns to the home dock. |

---

## 📱 4. Key Feature Walkthrough

### A. Weather & Air Quality (AQI) Hub
* Located under the digital clock (e.g., `☁ 27° Overcast  •  API 106 Unhealthy`).
* **Press UP (▲) then press OK** to launch the full Leanback weather interface:
  1. **Current Conditions**: Temperature, "Feels Like", humidity, wind velocity, UV index, sunrise and sunset times.
  2. **Official Air Quality Index (JAS Malaysia APIMS / IPU)**: Real-time ground data directly from the Department of Environment (JAS) APIMS portal with official Malaysian scale colors (Good: Green, Moderate: Sky Blue, Unhealthy: Orange, Very Unhealthy: Red, Hazardous: Maroon) and official health guidance.
  3. **12-Hour Hourly Forecast**: Hourly forecast cards with precipitation chance percentages.
  4. **7-Day Outlook**: Daily weather forecast across the upcoming week.

📍 **Manual Location Setup Directly on TV Remote (For Standalone APK Installs):**
Users who install the APK manually or relocate **DO NOT NEED A COMPUTER OR AI** to configure their station:
1. On the home screen, press **UP (▲)** and press **OK** on the weather pill to open the Weather Dashboard.
2. Press **UP (▲)** again to highlight the **📍 Tukar Stesen / Change** button next to the location name.
3. Press **OK**. A dialog modal will appear with 30+ official Malaysian DOE stations across all states:
   - *Putrajaya, Cheras (KL), Batu Muda (KL), Petaling Jaya, Shah Alam, Klang, Banting, Bandaraya Melaka, Alor Gajah, Bukit Rambai, Seremban, Nilai, Port Dickson, Larkin (JB), Pasir Gudang, Batu Pahat, Kluang, Tangkak, Seberang Jaya (Penang), Minden (USM), Balik Pulau, Ipoh, Taiping, Seri Manjung, Alor Setar, Sungai Petani, Langkawi, Kangar, Kuantan, Temerloh, Kuala Terengganu, Kemaman, Kota Bharu, Kota Kinabalu, Sandakan, Tawau, Kuching, Miri, Bintulu, Sibu, Labuan*.
4. Scroll with the remote **UP / DOWN** arrows to select your station and press **OK**.
5. Both the official JAS IPU readings and the JAKIM prayer timetable immediately calibrate to your chosen city!

### B. Islamic Prayer Timetable, Daily Ayat & Localized Khutbah Minbar 🕌
* **6-Phase Prayer Timetable**: Subuh, Syuruk, Zohor, Asar, Maghrib, and Isyak synchronized with your local time zone.
* **Active Window Glow**: The current active prayer window illuminates in bright gold/emerald green.
* **🕌 Localized Friday Sermons (Khutbah Minbar)**:
  - Weekly official Friday sermon excerpts automatically tailored to your state/authority:
    - **JAWI W.P. Putrajaya & Kuala Lumpur**
    - **JAIS Selangor**
    - **JAINJ Johor**
    - **JAIPP Penang**
    - **MUIS Singapore**
    - **Kemenag RI & NU Online (Indonesia)**
    - All Malaysian states (Perak, Kedah, Perlis, N. Sembilan, Terengganu, Kelantan, Sabah, Sarawak).
  - Fetched dynamically from `minbar.gosolat.com` and `islam.nu.or.id` with a rich offline fallback database.
* **🎛️ Dual Display Mode Switcher**:
  - Choose between **Doa Harian (Daily Quranic Ayat & Translation)** and **Khutbah Minbar (Friday Sermon)** anytime from **Modern Settings (⚙️)** $\rightarrow$ **Skrin Utama** $\rightarrow$ **Pilihan Paparan**.
* **🔍 Frosted Glass Detail Dialog**:
  - Press **OK** on the remote while focused on the Ayah/Minbar shelf to view the full sermon excerpt, source authority, date, and toggle display modes instantly with one click.
* **🎮 Bi-Directional DPAD Navigation**:
  - `▲ (Up)` from Dock apps slides directly onto the Ayah/Minbar shelf (highlighted in luminous cyan glass).
  - `▲ (Up)` from Ayah/Minbar ascends straight to the Top Bar Settings cog.
  - `▼ (Down)` steps back down through the shelf and onto your dock tiles.

### C. Dynamic 4K Wallpapers & Category Switcher
* **Curated Themes**: Switch your home background between curated 4K categories directly from the remote:
  - 🌿 **Natural (Default / Bing Places)**: World landmarks, mountains, lakes, and nature landscapes with informative location stories updated weekly.
  - 🇲🇾 **Malaysia (Curated 14 States & Federal Territories)**: Pick wallpapers specifically for any of Malaysia's 14 states or rotate all states together via a dedicated sub-dialog:
    - 📍 **Johor**: Pulau Rawa Mersing & Tropical Coastline.
    - 📍 **Kedah**: Langkawi Sky Bridge Mount Mat Cincang & Emerald Seas.
    - 📍 **Kelantan**: Pantai Cahaya Bulan & Pine Groves of Pantai Senok Kota Bharu.
    - 📍 **Melaka**: UNESCO Historic City & Melaka River Night Promenade.
    - 📍 **Negeri Sembilan**: Pantai Cahaya Negeri & Teluk Kemang Port Dickson.
    - 📍 **Pahang**: Cameron Highlands Rolling Tea Plantations & Bharat Tea Valley.
    - 📍 **Perak**: Ipoh Colonial Heritage Town, Kellie's Castle & Kek Lok Tong Limestone Cave.
    - 📍 **Perlis**: Gua Kelam Limestone Cavern & Nakawan Range Foothills.
    - 📍 **Pulau Pinang**: Georgetown UNESCO Heritage Streets & Kek Lok Si Temple.
    - 📍 **Sabah**: Mount Kinabalu Peak & South Peak Kundasang.
    - 📍 **Sarawak**: UNESCO Mulu Pinnacles, Kuching Waterfront & Miri Coastline.
    - 📍 **Selangor**: Batu Caves Sri Subramaniar & Shah Alam Blue Mosque.
    - 📍 **Terengganu**: Pulau Perhentian Marine Haven & Kuala Terengganu Crystal Mosque.
    - 📍 **Wilayah Persekutuan**: Petronas Twin Towers KLCC & Kuala Lumpur City Nightscape.
  - 🏙️ **Skyscraper**: Famous modern architecture and skylines (Manhattan NYC, Chicago River, Dubai Marina, Hong Kong Cityscape).
  - 🌌 **Deep Space**: Vibrant nebulae, starfields, and cosmic vistas.
  - 🌊 **Ocean & Coastal**: Pristine coastal horizons, waves, and azure seas.
  - 🔲 **Minimalist Dark**: Refined deep textured aesthetic, power-efficient and OLED-safe.
* **📂 State Picker Sub-Dropdown (Clean TV UI)**:
  - Click **`🇲🇾 Malaysia (Pilih Negeri ▶)`** on the wallpaper menu to open a leanback dialog listing all 14 Malaysian states.
  - Your active selection is displayed right on the main menu (e.g. `🇲🇾 Malaysia: Johor (Tukar Negeri ▶)`).
  - The TV shelf automatically highlights the location, state name, and historical story of each Malaysian landmark!
* **📲 Phone QR Code Wallpaper Upload**:
  - Select **`📲 Upload Wallpaper via Phone (QR Code)`** from the wallpaper options to display a clean QR code and local web link.
  - Scan with any smartphone on the same Wi-Fi network to upload custom family photos, vacation snapshots, or artwork directly to your TV without cloud accounts or third-party apps.
* **📱 On-TV Photo Manager & Single Wallpaper Lock**:
  - After uploading photos, easily manage them right from your phone browser or through the TV D-pad menu: **`🗑️ Urus Gambar Dimuat Naik (Pilih / Padam)`**.
  - **Lock Single Wallpaper**: Choose **`📌 Set as Single Wallpaper`** to lock a single photo permanently as your background without rotating.
  - **Rotate All Photos**: Choose **`🔄 Rotate All Uploaded Photos`** to cycle through all custom photos in slideshow mode.
  - **Delete Specific / Clear All**: Delete individual uploaded photos or wipe all custom images with one click.
* **🖼️ Clean View for Custom Wallpapers**:
  - When personal custom wallpapers are active, the left-hand text shelf / description is completely hidden (`View.GONE`) so your personal photos are displayed edge-to-edge without being obscured by location captions.
* **Instant Shift**: Select **🔀 Next Wallpaper** anytime to immediately cycle to the next photo in your active category.

### D. StandBy Ambient Mode & Smart Power Saving
* **10 Minutes Idle**: Enters an ambient OLED-safe black StandBy screensaver displaying large 110sp numerals, Islamic Hijri date, and the upcoming prayer window.
* **2 Hours Later**: If left untouched, the TV display enters sleep standby (`GLOBAL_ACTION_LOCK_SCREEN`) to save power.
* **Wake Up**: Press any button on the TV remote to wake the TV immediately.

### E. 16:9 Floating Azan Notification & Audio Settings 🕌
A respectful spiritual living room feature synchronized live with official JAKIM Malaysia E-Solat schedules:
* **Floating System Overlay**: At the precise time of prayer, an elegant 16:9 frosted glass notification card automatically appears on screen over running apps (Netflix, YouTube, media players) without crashing or interrupting playback.
* **All-In-One Control Center**: Open via the **Candy Box (㗊)** icon on the top bar or by **Holding the HOME button**:
  1. **A. Master Popup Switch**: Toggle Azan notification overlays globally *ON* or *OFF*.
  2. **B. Individual Prayer Toggles**: Set alerts separately for each of the 6 prayer periods: *Subuh, Syuruk, Zohor, Asar, Maghrib, Isyak*.
  3. **C. Azan Audio Presets**: Choose your preferred Azan audio: *Masjid As-Syakirin KLCC, Makkah Mukarramah, or Madinah Munawwarah*.
  4. **D. Live Test Mode**: Click **▶️ Test Azan Popup Now** to trigger an instant test overlay and verify sound volume.

### F. Frosted Glass Settings Dropdown Menu ⚙️
Clicking the **Settings (gear icon ⚙️)** on the top bar reveals an Apple tvOS-style floating glass menu:
* 🖼️ **Wallpaper**: Launches the category selector dialog (*Natural, Skyscraper, Deep Space, Ocean, Minimalist Dark, Next Wallpaper*). Preferences persist across reboots.
* 📍 **Location API Haze**: Opens a selector for 30+ official Malaysian DOE (JAS APIMS) air quality monitoring stations.
* 🕌 **Location Waktu Solat**: Opens a selector for 35+ official JAKIM prayer zones across all Malaysian states.
* 🔊 **Sound Effect**: Displays current audio theme (e.g. `Sound: PlayStation 5`) and launches the console sound profile selector with live previews.
* 🔄 **Check for Updates**: Checks GitHub directly for newer releases or patches without requiring a PC.
* ⚙️ **All Settings**: Opens standard Android TV system preferences.
*(Shortcut: Long-pressing the Settings button on your remote directly launches standard Android settings).*

### G. Console Sound Effects & Mute Mode (PS5, Switch, Xbox) 🎮🔊
Customize your TV navigation audio with iconic gaming console profiles or mute all sounds entirely:
* **PlayStation 5 (Default)**: Sleek, high-definition acoustic whooshes and tactile clicks.
* **Nintendo Switch**: Sharp mechanical tactile clicks and cheerful confirmation snaps.
* **Xbox Series X/S**: Resonant warm mid-bass navigation pulses and two-tone rising chime.
* **Muted / Off**: Complete silence for whisper-quiet late night movie sessions.
* **Instant Audio Audition**: Tapping any theme inside the dialog immediately plays its signature chime.
* **Persistent Preferences**: Settings remain saved across TV restarts via SharedPreferences.

### H. On-TV OTA Updates & Auto-Patch Checker 🔄🚀
Glass TV Home features a built-in OTA update system so you can update directly on your TV without a computer:
* **Manual Check**: Navigate to **Settings (⚙️) $\rightarrow$ Check for Updates**. The app contacts the GitHub release manifest and notifies you if a new version is available.
* **Silent Startup Check**: The launcher quietly checks for new versions in the background when booted. If a new patch is found, a changelog dialog is presented.
* **1-Click Download & Install**: Clicking **Muat Turun & Pasang** downloads the update file locally and launches Android TV's native package installer.
* **Data Preserved**: Updating preserves all apps, dock layout, and custom configurations.

---

## 📲 5. Mobile Phone Setup (Without Any PC / Laptop)

You do **not** need a computer to install Glass TV Home.

### Method 1: Official 'Glass TV Installer' App (Zero-Config & Easiest) 🌟
We built a dedicated companion installer app for Android phones and tablets that embeds the launcher and installs it over Wi-Fi:

* **Download Companion App**: [**`glass-tv-installer.apk`**](https://github.com/aqmangge/glass-tv-home/raw/main/release/glass-tv-installer.apk) (~5.2 MB)

1. **Enable Debugging on TV**:
   - Go to TV **Settings ⚙️** $\rightarrow$ **About** $\rightarrow$ Click **Android TV OS Build** 7 times.
   - Go to **Developer options** $\rightarrow$ Enable **USB debugging**.
2. **Same Wi-Fi**: Connect your phone and TV to the same Wi-Fi network.
3. **Open Glass TV Installer on Phone**:
   - Tap **`🔍 Imbas Android TV di Rangkaian Wi-Fi`** to discover your TV automatically (or type the TV IP).
4. **1-Click Install**:
   - Tap **`🚀 Pasang Terus ke Android TV`**.
   - On the TV screen, accept the *"Allow USB debugging"* prompt.
   - The app streams and installs Glass TV Home directly to your TV.
5. **Launch**:
   - Tap **`📺 Buka Glass TV Home di Skrin TV`**!

> 💡 **Local Web Server Option**: If debugging is unavailable on your TV, tap **`🌐 Pelayan Web TV`** on your phone, then open **Downloader** or a web browser on your TV and navigate to the displayed local IP URL to download and install.

---

### Method 2: Android Phone via 'Bugjaeger' (For Advanced Debloating)
The **Bugjaeger Mobile ADB** app (free on Google Play Store) allows your phone to act as an ADB controller over Wi-Fi:

1. Connect both phone and TV to the **same Wi-Fi**.
2. Open **Bugjaeger**, tap the **Connect 🔌** icon, enter your TV IP (e.g. `192.168.68.102:5555`), and tap **Connect**.
3. Accept the *"Allow USB debugging"* prompt on your TV screen.
4. Go to the **Packages 📦** tab in Bugjaeger, tap **+**, and select the downloaded `app-release.apk` to install it directly.
5. In the **Commands ⚡** tab, run these commands to block ads and debloat:
   ```bash
   settings put global private_dns_specifier dns.adguard-dns.com
   settings put global private_dns_mode hostname
   pm disable-user --user 0 com.mitv.tvhome
   pm disable-user --user 0 com.xiaomi.mitv.advertise
   ```

---

### Method 3: iPhone (iOS) Users via Muse.ai Assistance
**Yes! iPhone users can easily install the APK and remove unwanted TV apps with AI assistance from Muse.ai.**

Because Apple App Store sandbox policies do not permit direct ADB shell apps on iOS, iPhone users can simply open Safari and visit [**muse.ai**](https://muse.ai) (or your preferred web AI assistant) for step-by-step guidance:

#### 1. Ask Muse.ai in iPhone Safari:
Prompt Muse.ai:
> *"I use an iPhone and have an Android TV [insert TV model, e.g. Xiaomi / TCL / Sony]. Give me the easiest instructions to install https://github.com/aqmangge/glass-tv-home on my TV, and tell me which bloatware apps are safe to disable so my TV runs fast and ad-free."*

#### 2. Install APK to TV:
* **Option A (Downloader on TV)**: Install **Downloader by AFTVnews** from your TV's Play Store and type the direct APK link:  
  `https://github.com/aqmangge/glass-tv-home/raw/main/release/glass-tv-home.apk`
* **Option B (LocalSend from iPhone)**: Use **LocalSend** (free on iOS App Store & Android TV Play Store) to beam `glass-tv-home.apk` over Wi-Fi and install.

#### 3. Debloat Unwanted Apps & Block Ads:
* **Disable Manufacturer Bloatware**: In TV **Settings ⚙️** $\rightarrow$ **Apps** $\rightarrow$ **See all apps**, select the unnecessary manufacturer apps identified by Muse.ai and select **Disable**.
* **Block Ads Without a PC (AdGuard Private DNS)**:
  1. On TV remote: **Settings ⚙️** $\rightarrow$ **Network & Internet** $\rightarrow$ **Private DNS**.
  2. Choose *Private DNS provider hostname* and enter:
     ```text
     dns.adguard-dns.com
     ```
  3. Click **Save**. All intrusive home screen ads and tracking domains are instantly blocked!

---

## 🛠️ 6. Alternative: Manual Installation via USB Flash Drive

If you prefer using a physical USB drive:
1. Download [**glass-tv-home.apk**](https://github.com/aqmangge/glass-tv-home/raw/main/release/glass-tv-home.apk) (also in [`release/glass-tv-home.apk`](release/glass-tv-home.apk)) onto your USB drive.
2. Plug the USB flash drive into your Android TV or Xiaomi Mi Box.
3. Open any file explorer app on the TV (e.g., *File Commander* or *FX File Explorer* from Google Play Store).
4. Click `app-release.apk` and select **Install** (allow *"Install unknown apps"* if prompted).
5. Press the **Home** button on your remote and choose **Glass TV Home** as *Always*.

---

## ❓ 7. Frequently Asked Questions (FAQ)

### Q1: Can I run Claude Code on a mobile phone (iOS / Android), and does the iOS sandbox block local network access?
* **CLI / Node.js Requirement:** Claude Code (`@anthropic-ai/claude-code`) is an interactive terminal tool that requires a full **Node.js 18+** shell environment. iOS sandbox policies strictly forbid running Node.js engines and local arbitrary shell executables directly on an iPhone.
* **Cloud vs Local Network Barrier:** Claude Code or any cloud-hosted AI (e.g., web Claude or web ChatGPT) executes on remote cloud servers. Cloud servers **CANNOT enter your private home Wi-Fi network** to access your TV's private IP (`192.168.x.x`) due to your home router's NAT firewall.
* **Solution:** ADB commands must be initiated by a local device on your home Wi-Fi (such as a laptop/PC running Google Antigravity or Claude Code, or an Android phone running Bugjaeger).

---

### Q2: Is there an AI that can install and do all tasks directly without the user installing the APK manually?
* **YES, when run on a computer/laptop on the same Wi-Fi (Google Antigravity / Claude Code / Cursor):**
  The AI connects to the TV over wireless ADB (`adb connect 192.168.x.x`), builds the APK, installs it, removes bloatware, and verifies everything with automated screenshots. The user does not need to touch or open any APK file manually!
* **On mobile phones without a PC:** You simply use a helper app (Bugjaeger on Android, or LocalSend on iPhone) to transfer and install the APK.

---

### Q3: If a user only installs the APK file, do they have full access with the TV remote, or do they still need AI access?
* **100% STANDALONE! YOU ONLY NEED YOUR TV REMOTE, ZERO AI REQUIRED!**
* Glass TV Home is a **native Android TV application** (Native Kotlin/AndroidX).
* Once the APK is installed on the TV:
  - **Full Remote Control**: The D-Pad (Up/Down/Left/Right/OK), Back, and Home buttons navigate everything natively.
  - **All smart features run locally**: Clock, Hijri dates, prayer timetable, weather & AQI (APIMS), 10-minute screensaver, and 2-hour auto sleep operate entirely inside the TV without requiring an AI agent, computer, or active subscription.
  - Family members and non-technical users can use the TV just like a standard commercial smart TV.
* **The AI's role is purely as a "mechanic/technician"** during the initial setup to automate installation and debloating. Once installed, Glass TV Home is 100% independent.

---

### Q4: Can I use this on a Unifi TV Box (TM Unifi Plus Box), and will it void my warranty?
* **1. Can it be installed on a Unifi TV Box?**
  - **YES, ABSOLUTELY!** TM Unifi Plus Boxes (both the white Skyworth HP40A3 and black ZTE models) run official Android TV.
  - You can install the Glass TV Home APK via USB flash drive, Downloader app, Send Files to TV, or wireless ADB.
  - **The official unifi TV app works normally:** You won't lose access to TM's live channels, sports, or on-demand shows because the **unifi TV** app will automatically show up in your apps list and can be pinned to your favorite dock row.
* **2. Will it void TM's warranty?**
  - **NO, YOUR WARRANTY REMAINS 100% SAFE AND INTACT.**
  - **No Rooting / Zero Firmware Modification:** Glass TV Home operates as a standard Android user-space application (just like installing Netflix or YouTube). It does not alter system firmware partitions or tamper with hardware.
  - **100% Reversible:** If you ever need technical support from TM or want to return the box, simply go to:
    `Settings -> Apps -> Glass TV Home -> Uninstall`
    or perform a standard Factory Reset. The box instantly reverts to the original TM factory launcher with zero traces left behind.

---

## 📄 License & Credits
* Built upon [CleanYourTV](https://github.com/Gathin23/cleanyourtv) © Gathin (MIT License).
* Outfit font by SIL Open Font License.
* Weather and AQI data powered by Open-Meteo & Malaysia Department of Environment (APIMS) standards.
