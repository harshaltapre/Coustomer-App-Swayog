# SWAYOG Customer Mobile App ☀️

A premium, state-of-the-art Android application built to help solar customers monitor real-time energy generation, track system performance with interactive weekly/monthly/yearly graphs, manage maintenance services, check local solar-weather parameters, and handle invoices and billing. The application utilizes a dark-mode first design system with glassmorphic UI components, smooth GPU-accelerated micro-animations, and full offline caching capabilities.

---

## 📱 Features & UI Screens

### 1. Authentication & Security Screen (`LoginScreen`)
* **Welcome Back Portal:** Sleek dark-mode interface with floating inputs for seamless access.
* **GPU-Optimized Logo Animation:** Animated solar logo featuring a rotating sun vector and a pulsing background glow. Configured via `Modifier.graphicsLayer` to bypass recomposition loops, executing entirely on the GPU draw-phase at a lock-smooth 60fps.
* **Biometric Authentication:** One-tap login via the physical device fingerprint sensor (`BiometricPrompt`). Restores login credentials securely from the storage.
* **Graceful Credential Storage Fallback:** Stores session and credential tokens. Uses `EncryptedSharedPreferences` for secure AES-256 storage, with an automatic try-catch fallback to standard `SharedPreferences` if keystore initialization fails.
* **Dynamic Network Error Handling:** Intercepts and formats network connection failures (timeouts, unknown host, connection refused) into user-friendly diagnostic banners.

### 2. Main Interface & Navigation (`MainActivity`)
* **Edge-to-Edge Experience:** Fully immersive styling where the system status bar and navigation bars match the app's dark theme.
* **Top-Level Scaffold:** Features a transparent container over a custom vertical gradient background (`BackgroundDark` to `0xFF0D1220`).
* **Glassmorphic Navigation Bar:** Bottom navigation bar styled with a curved top corner clip, standard active-state indicator halos (12% opacity brand orange), and responsive page transition fades.

### 3. Personal Dashboard (`DashboardScreen`)
* **Time-of-Day Greetings:** Personalized header greeting ("Good Morning", "Good Afternoon", "Good Evening") based on the device clock, displaying the user's first name next to a quick logout icon.
* **Swipe-to-Refresh:** Pull-to-refresh container triggers instant background database synchronization.
* **Live Generation Gauge:** Custom 270-degree canvas arc tracking real-time energy production. Visualizes current generation in kW out of maximum system generation capacity (e.g. `4.2 kW / 5.4 kW capacity`).
* **Core Metrics Grid:** Quick-access metric cards showcasing:
  * **Daily Output:** Cumulative energy generated today in kWh.
  * **Lifetime Output:** Total historical energy generated in kWh.
  * **Inverter Health Sync:** Connection state and status sync health (Online/Offline glowing indicator with timestamp).
* **Live Weather Widget:** Replaces carbon emission chips with real-time local weather. Shows dynamic temperature (°C), wind speed (km/h), and sky conditions (e.g., "Partly Cloudy", "Sunny", "Overcast") dynamically mapped to the solar location coordinates.
* **System Details Info-Card:** Showcases physical configuration metrics (system kW size, panel brand, inverter model) alongside a glowing live connection indicator dot (Green for Online, Red for Offline).

### 4. Solar Telemetry & Performance Tracker (`TrackerScreen`)
* **Generation Tracking Dashboard:** Completely replaces the static installation journey. Displays interactive solar analytics.
* **Multi-Period Analytics Selector:** Smooth tabs to switch between:
  * **Real-time (Today):** Live power yield (kW) throughout the day.
  * **Weekly:** Daily generation (kWh) for the last 7 days.
  * **Monthly:** Generation (kWh) across the days of the current month.
  * **Yearly:** Generation (kWh) across the months of the current year.
* **Interactive Charting Engines:**
  * **Area Chart (Real-time):** Smooth Bezier-curve line chart mapping power output over time with a glowing eco-green gradient fill under the line.
  * **Bar Charts (Weekly/Monthly/Yearly):** Styled rounded vertical bar charts in brand sky-blue mapping energy yield over time.
  * **Dynamic Value Tooltips:** Dragging a finger across the charts renders an overlay tooltip showing precise timestamped generation levels.

### 5. Service & Maintenance Desk (`ServiceRequestsScreen`)
* **Service Request Log:** Access list of previous cleaning and repair requests with color-coded status badges (Pending, Assigned, Completed).
* **Creation Panel:** Dropdown selection for issue types (Cleaning, Maintenance, Electrical, Inverter, Other) with input text fields.
* **Camera & Media Attachment:** Directly capture or select photos of solar panels to upload with the service request.
* **Multipart Image Upload:** Packs attachments and text parameters as `multipart/form-data` for backend server file hosting.
* **Offline Caching & WorkManager Sync:** If internet connectivity is lost, requests are saved locally in the SQLite database. A background `ServiceRequestSyncWorker` automatically retries and uploads requests once connectivity is restored.

### 6. Billing & Payments Hub (`PaymentsScreen`)
* **Outstanding Balance Card:** Features a high-contrast gradient card displaying outstanding invoices.
* **Razorpay Gateway Integration:** Launch payments directly inside the app using Razorpay's checkout module.
* **Transaction History:** Lists historical payment receipts with payment method descriptors, dates, and verification status codes.

---

## 🛠️ Architecture & Tech Stack

The app is written in **Kotlin** and follows the **MVVM (Model-View-ViewModel)** architecture pattern:

* **Jetpack Compose:** Declarative UI rendering engine.
* **Dagger Hilt:** Dependency Injection framework managing lifetimes of network, database, and repository modules.
* **Room Database:** SQLite ORM caching remote data locally to support instant app loading and offline capabilities.
* **Retrofit & OkHttp:** Network client managing API calls, automatic authorization token injection (`AuthInterceptor`), and connection/read timeout thresholds (30 seconds).
* **WorkManager:** Manages background workers with network constraints to guarantee data upload consistency.

---

## 🔌 API & Server Configuration

### Local Network Connection
By default, Retrofit is configured to connect to the backend server via the local host IP:
```kotlin
// AppModule.kt
.baseUrl("http://192.168.1.12:4000/")
```
* **Host Machine local IP:** `192.168.1.12` (running on port `4000`).
* **Why this is used:** Physical devices connected to the same local Wi-Fi router cannot connect to `localhost` or `10.0.2.2` (which are loopback addresses). Using the host machine's Wi-Fi IP allows both the Android Emulator and physical devices to interact with the backend server.

### Robust Crash Prevention (Firebase)
* **Unconfigured Firebase Protection:** The application checks if Firebase is initialized on the device (`FirebaseApp.getApps(context).isNotEmpty()`) before attempting to sync FCM tokens. This prevents startup crash loops if the app is compiled without a `google-services.json` configuration file.
"# Coustomer-App-Swayog" 
