# Product Requirement Document (PRD) - SWAYOG Customer Mobile App

## 1. Introduction & User Personas
This PRD outlines functional requirements for the **SWAYOG Customer Mobile App**.

### 1.1 User Persona: Resident Solar Owner
* **Goal**: Track solar panels output, request cleanings when output drops, and pay AMC invoices.
* **Pain Point**: System state visibility is lost when away from the desktop web application.

---

## 2. Functional Requirements

### 2.1 User Authentication & Security
* **FR-01**: The system must authenticate customers using standard credentials synced with the Postgres backend.
* **FR-02**: The application must support biometric fingerprint unlock once a primary login succeeds.
* **FR-03**: Secure credentials storage using `EncryptedSharedPreferences` with automatic standard SharedPreferences fallback if KeyStore fails.
* **FR-04**: Session Timeout: The app locks and prompts for biometrics if it remains in the background for more than 15 minutes.

### 2.2 Dashboard (Home)
* **FR-05**: Hero trajectory progress banner tracking installation phases (from Site Survey to Handover).
* **FR-06**: Live Energy circular gauge tracking active kW output relative to max kW system capacity.
* **FR-07**: Solar metrics grid rendering Daily Yield (kWh), Lifetime Yield (kWh), and Sync Health status (online/offline).
* **FR-08**: Dynamic weather card displaying temperature (°C), cloud conditions, and wind speed (km/h) sourced from Open-Meteo's weather APIs.

### 2.3 Interactive Telemetry Analytics
* **FR-09**: Period Selection Tabs: Real-time, Weekly, Monthly, and Yearly options.
* **FR-10**: Spline Area Chart for Real-time view tracking power output values (kW) throughout the current day.
* **FR-11**: Rounded Bar Charts for Weekly, Monthly, and Yearly views mapping generation yields (kWh) over time.
* **FR-12**: Tooltip details rendering on touch/drag gesture showing exact time/yield values.
* **FR-13**: Period routing mapping: Weekly and Monthly choices request `"daily"` history from the backend server to avoid unsupported API errors. Weekly filters down to the last 7 daily points.

### 2.4 Service Desk & AMC Tracker
* **FR-14**: Service request history list with colored state status badges (Pending, Assigned, Completed).
* **FR-15**: Submit ticket form with dropdown categories, details description, geolocation coordinate capture, and camera/gallery attachments.
* **FR-16**: Image optimization compressing JPEG attachments under 500KB.
* **FR-17**: Offline Queue: Offline service requests are stored locally with `isSynced = false`. WorkManager executes the background sync task as soon as internet connectivity returns.

### 2.5 Billing Ledger & Payments
* **FR-18**: Invoices list displaying invoice date, amount, type, and payment status.
* **FR-19**: Pay outstanding balance triggers Razorpay Android SDK checkout window.
* **FR-20**: Confirm payments via POST verification endpoints and refresh billing lists.

---

## 3. Non-Functional Requirements (NFR)
* **Performance**: Jetpack Compose graphics drawing bypasses recomposition loops to ensure smooth 60fps animations.
* **Network efficiency**: Offline-first caching with Room DB ensures instant page loading without loading screens when reading cached data.
* **SDK Compatibility**: Min SDK 24, Target SDK 34.
