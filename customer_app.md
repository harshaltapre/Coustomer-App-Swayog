# SWAYOG Customer Mobile Application

## Technical Specification & UI/UX Design Blueprint

This document defines the architecture, user experience (UI/UX) design system, screen flows, and database API synchronization mechanism for the **SWAYOG Customer Mobile Application**. The application is designed to give solar customers a premium, high-fidelity experience to track their solar installation journey, monitor energy production, manage maintenance, and handle payments.

---

## 1. Executive Summary & App Concept

* **Goal**: Translate the web-based Customer Dashboard into a high-performance, native-feeling Android application with an optimized mobile interface.
* **Target Platform**: Android (Kotlin & Jetpack Compose for native, or Flutter for clean cross-platform performance).
* **Core Objectives**:
    1. **Unified Authentication**: Single sign-on matching the web platform using secure JWT tokens.
    2. **Solar Generation Visibility**: Easy tracking of energy data (integrated with Growatt ShineServer/SolisCloud via the backend).
    3. **Real-Time Installation Tracker**: Mobile-first timeline showing the 12 stages of system installation.
    4. **Service & AMC Management**: Geo-tagged maintenance logging and cleaning reminders.
    5. **Seamless Payments**: In-app payment capabilities via Razorpay SDK.
    6. **Offline Resilience**: Full offline-first design caching crucial system data local to the device.

---

## 2. UI/UX Design System (Premium Solar Theme)

The customer app utilizes a dark-mode first, glassmorphic visual style that represents clean energy, solar power, and technical precision.

### A. Color Palette

| Token | Value (Hex) | Purpose / Accent |
|---|---|---|
| **Background Dark** | `#0B132B` | Core app canvas |
| **Surface Dark** | `#1C2541` | Card backgrounds, dialogs, inputs |
| **Primary Solar** | `#FF6B00` | Energy generation, solar metrics, action buttons |
| **Eco Accent** | `#10B981` | Completed states, active status, system operational |
| **Info Accent** | `#0284C7` | Dispatch statuses, loading indicators |
| **Text Primary** | `#FFFFFF` | Headings, heavy contrast text |
| **Text Secondary**| `#94A3B8` | Subtext, timestamps, labels |

### B. Typography & Micro-Animations

* **Font Family**: `Outfit` (Primary headings for modern look) and `Inter` (Secondary text for high readability).
* **Micro-Animations**:
  * **Pulse & Spin**: Loading spinners and solar generation progress gauges.
  * **Slide Transitions**: Screen switching using subtle slide-in / slide-out transitions.
  * **Progress Fill**: Installation tracker filling up dynamically with a bouncing overshoot animation.

---

## 3. Authentication & Login Flow

The customer logs in using the unique credentials generated during onboarding (synced with the backend database).

```
[Customer Opens App] -> [Token Verification] 
                            |-- Valid -> [Enter Dashboard]
                            |-- Invalid/Expired -> [Login Screen]
```

### A. Login Screen Specifications

* **Elements**:
  * Sleek glassmorphic card with the SWAYOG Logo.
  * `Login ID` or `Email` input field.
  * `Password` input with eye icon to toggle visibility.
  * "Remember Me" toggle (saves encrypted login data).
  * **Biometric Quick Unlock**: Button to authenticate using fingerprint or face ID (via Android BiometricPrompt API) once a first successful login is completed.
* **Validation**: Frontend verification for valid email format and minimum password length before making API requests.

### B. Token Storage & Session Sync

1. On successful login, the API returns:
    * `accessToken` (JWT - short-lived, 15 min expiry)
    * `refreshToken` (UUID string - long-lived, 30 days expiry)
2. The application stores the tokens inside **EncryptedSharedPreferences** (utilizing Android KeyStore system to prevent rooting/interception exploits).
3. **Interceptor System**: All HTTP requests are processed through an API interceptor. If a request returns `401 Unauthorized`, the app suspends active calls, calls the `/api/v1/auth/refresh` endpoint to exchange the `refreshToken` for a new `accessToken`, and retries the original request seamlessly.

---

## 4. Screen-by-Screen Breakdown (Mobile Layout)

### Screen 1: Dashboard (Home)

* **Hero Progress Section**:
  * Visual progress banner mapping the current stage of installation.
  * Horizontal progress bar showing completed steps (e.g., `4 of 12 steps completed (33%)`).
  * "Launch Tracker" quick-link button.
* **System Details Card**:
  * `System Size` in kW (large typography).
  * `Panel Brand` and `Inverter Brand` logos/text.
  * `Live Status` Indicator (online/offline) dynamically read from inverter sync.
* **AMC & Cleanings Card**:
  * `AMC Status` (Active / Expired badge).
  * Cleanings completed vs pending count (e.g., `2 completed / 2 pending`).
* **Carbon Offset Visualizer**:
  * Dynamic calculation showing equivalence: `🌲 14 Trees Planted` and `💨 340kg CO2 Prevented`.

### Screen 2: Detailed Installation Tracker

* **Interaction**: Vertical scrollable timeline listing all **12 installation phases**:
    1. *Site Survey*
    2. *Document Collection*
    3. *Approval and Advance Payment*
    4. *Licensing*
    5. *2nd Instalment*
    6. *Procurement*
    7. *Vendor Selection*
    8. *Installation*
    9. *WCR (Work Completion Report)*
    10. *3rd Instalment*
    11. *Meter Installation & Subsidy Redeem*
    12. *System Handover*
* **Visual States per Step**:
  * `Green Checkmark`: Step is fully completed.
  * `Orange Pulse`: Step is currently in progress.
  * `Gray Indicator`: Step is locked/pending.
* **Details Drawer**: Clicking a step opens a bottom sheet showing dates, technician notes, or uploaded verification photos.

### Screen 3: Material Dispatches

* Lists all inventory items shipped to the customer's site.
* **Elements**:
  * Total Material Value summary card: `₹2,34,500`.
  * List of entries showing:
    * Material name (e.g., *Tata Power 540W Solar Modules*).
    * Dispatched date (formatted: `dd MMM yyyy`).
    * Quantity (bold monospaced numbers).
    * Unit price & Total calculated cost.
    * Admin delivery notes (e.g., "Left near security gate").

### Screen 4: Service Requests & AMC visits

* **My Service Tickets**:
  * Active/Closed list showing past issues, request types, and current status badges (`pending`, `assigned`, `completed`).
* **"Submit Service Request" Bottom Sheet**:
  * **Category Dropdown**: Panel Cleaning, Inverter Connection issue, Structural check, Electrical wiring.
  * **Description Box**: Detailed description of the issue.
  * **Geo-Location Tagging**: Integrates FusedLocationProviderClient to automatically attach `latitude` and `longitude` coordinates of the system site (ensures technicians find the exact inverter site).
  * **Media Attachment**: Click photos or record videos of physical damages to upload directly.
* **AMC Visits Schedule**:
  * Upcoming scheduled visit details (Technician name, phone, scheduled time slot, and monthly cleaning number).

### Screen 5: Payments & Ledger (Razorpay)

* **Ledger Card**: Displays the overall transaction ledger for the client installation.
  * Outstanding amount due.
  * Completed payments list with payment dates, transaction IDs, and methods.
* **Pay Outstanding Balance**:
  * Input payment amount.
  * "Proceed to Pay" button triggers the native **Razorpay Android Standard Integration SDK**.
  * Seamless transition to UPI, NetBanking, Card, or Wallet payments inside the app.
  * Webhook verification ensures payment status is reconciled on the PostgreSQL database instantly.

---

## 5. API & Database Sync Mechanism

To achieve a seamless interface and minimize network overhead, the app uses an **Offline-First Synchronization** strategy.

```
+---------------------------------------+
|          Jetpack Compose UI           |
+---------------------------------------+
                   ^
                   | (Observes Flow/LiveData)
                   v
+---------------------------------------+
|      Room Local Database Cache        |
+---------------------------------------+
                   ^
                   | (Syncs updates via WorkManager)
                   v
+---------------------------------------+
|      Retrofit / API Service Layer     |
+---------------------------------------+
                   ^
                   | (JSON / REST API)
                   v
+---------------------------------------+
|            PostgreSQL DB              |
+---------------------------------------+
```

### A. SQLite/Room Entity Definitions

* **`CustomerProfile`**: Caches the name, customer code, address, system size, and inverter credentials.
* **`ServiceRequest`**: Caches submitted and pending requests. Saves an `isSynced` boolean flag.
* **`DispatchRecord`**: Caches dispatched materials and costs.
* **`AmcVisit`**: Caches historical and upcoming maintenance logs.

### B. Sync Logic Flow (Periodic & Reactive)

1. **Read Operations (Offline-First)**:
    * When the user opens the app, the UI immediately loads cached data from the local Room database (instant load, no skeleton loaders needed).
    * A background network request is fired to fetch latest updates from `/api/v1/customer/stats` and `/api/v1/customer/installation`.
    * Upon successful response, the local database is updated, and Compose Reactivity instantly redraws the UI.
2. **Write Operations (Queue & Sync)**:
    * When submitting a Service Request or upload, the app writes it directly to the local database with `isSynced = false`.
    * A **WorkManager OneTimeWorkRequest** is queued.
    * If the phone is online, the sync worker runs immediately, sends the request to `/api/v1/customer/requests` via Retrofit, gets the response, and marks `isSynced = true` locally.
    * If offline, the worker halts. WorkManager automatically retries once internet connectivity is restored, utilizing network-state constraints to avoid draining battery.

### C. API Endpoint Mapping Reference

| Screen Event | HTTP Method | Endpoint | Request Body / Query Params |
|---|---|---|---|
| User Authentication | `POST` | `/api/v1/auth/login` | `{ "loginId", "password" }` |
| Token Refresh | `POST` | `/api/v1/auth/refresh` | `{ "refreshToken" }` |
| Get User Profile | `GET` | `/api/v1/customer/profile` | Header: `Authorization: Bearer <token>` |
| Fetch System Details & Stats | `GET` | `/api/v1/customer/stats` | Header: `Authorization: Bearer <token>` |
| Fetch Tracker Stages | `GET` | `/api/v1/customer/installation` | Header: `Authorization: Bearer <token>` |
| Fetch Material Dispatches | `GET` | `/api/v1/customer/dispatches` | Header: `Authorization: Bearer <token>` |
| Get Service Requests | `GET` | `/api/v1/customer/requests` | Header: `Authorization: Bearer <token>`, Query: `?status=pending` |
| Create Service Ticket | `POST` | `/api/v1/customer/requests` | `{ "serviceType", "description", "address", "latitude", "longitude", "preferredDate" }` |
| Create Payment Order | `POST` | `/api/v1/customer/payments/razorpay/order` | `{ "amount", "currency": "INR" }` |
| Verify Payment Receipt | `POST` | `/api/v1/customer/payments/razorpay/verify` | `{ "razorpay_order_id", "razorpay_payment_id", "razorpay_signature" }` |

---

## 6. Environment Configuration & Database Credentials

For the mobile application to successfully retrieve and synchronize the exact same dataset as the web dashboard, it must interface with the same environment variables, APIs, and PostgreSQL database endpoints. Below are the configurations derived from the system `.env` profiles.

### A. API Connection Endpoints
* **Development (Vite Frontend Proxy)**: `http://127.0.0.1:4000` (mapped via Vite's `VITE_API_BASE_URL` at `d:\intrnship\dashboard_swayog\.env`).
* **Android Emulator Host Redirect**: In the Android emulator codebase, replace `localhost` / `127.0.0.1` with `http://10.0.2.2:4000` to properly route network traffic to the locally running Express.js server backend.
* **Production Web URL (Staging)**: `https://swayog-dashboard-delta.vercel.app` (enforces CORS origin checks for incoming API requests).

### B. PostgreSQL Database Credentials
The application connects to two environments depending on the backend configuration:

#### 1. Local Database Environment (Docker or Local Postgres)
* **Host**: `127.0.0.1` (port `5432`)
* **Username**: `postgres`
* **Password**: `12345678`
* **Database Name**: `dashboard_swayog`

#### 2. Cloud Database (Production Staging - Neon Tech Serverless Postgres)
* **Database Pooler URL (`DATABASE_URL`)**:
  ```connection-string
  postgresql://neondb_owner:npg_4NYF3wHeqkOm@ep-red-poetry-apyaxvb1-pooler.c-7.us-east-1.aws.neon.tech/neondb?sslmode=require
  ```
* **Direct Connection URL (`DIRECT_URL`)**:
  ```connection-string
  postgresql://neondb_owner:npg_4NYF3wHeqkOm@ep-red-poetry-apyaxvb1.c-7.us-east-1.aws.neon.tech/neondb?sslmode=require
  ```

### C. JWT Authentication Secrets
To sign, authenticate, and decrypt tokens matching the web dashboard login handlers:
* **JWT Access Secret**: `7kPmNqRsTuVwXyZaBcDeFgHiJkLmNoPq8rStUvWxYz`
* **JWT Refresh Secret**: `9aBcDeFgHiJkLmNoPqRsTuVwXyZaBcDeFgHiJkL2mNo`
* **Access Token TTL**: `15m` (15 minutes)
* **Refresh Token TTL**: `7d` (7 days)

---

## 7. Safety, Security & Performance Guidelines

* **API Security**: HTTPS validation is enforced using SSL Pinning to prevent man-in-the-middle (MITM) inspection on unsecured public Wi-Fi networks.
* **Background Restrictions**: Location sensors (`latitude`/`longitude`) are requested *only* during active service request submission and are immediately turned off.
* **Media Compression**: Before uploading photos of service issues, the app compresses images to under `500KB` (JPEG) using local Android bitmap manipulation to save customer data and optimize server storage.
* **State Management**: Built using `ViewModel` and `Kotlin Coroutines StateFlow` to manage state changes during screen rotations or interruptions.

