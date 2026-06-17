# Privacy Policies - SWAYOG Customer Mobile App

## 1. Introduction & Overview
This Privacy Policy outlines how the **SWAYOG Customer Mobile App** collects, secures, uses, and shares personal data from residential and commercial solar arrays owners. We protect consumer data according to standard privacy laws.

---

## 2. Information Collection & Permissions

### 2.1 Explicit Device Permissions
* **Location Access**: The app requests location coordinates (latitude, longitude) via `FusedLocationProviderClient` strictly during the **Create Service Ticket** transaction flow. This location data helps field engineers locate physical solar panels. Geolocation services are closed as soon as the ticket submission resolves.
* **Camera and Gallery Access**: Required exclusively for users wishing to capture and attach damaged component photos to service tickets. No silent background media access is performed.
* **Biometric Hardware**: The app interfaces with Android's secure BiometricPrompt APIs. SWAYOG does not collect or transmit fingerprint scans or biometric profiles; matches are processed directly inside the secure hardware KeyStore.

### 2.2 Profile & Telemetry Data
* **User Profiles**: Name, contact phone numbers, email address, and system sizes are stored.
* **Telemetry Data**: Total yields (kWh) and active production values (kW) are gathered from Growatt/Solis cloud synchronization points to build analytics reports.

---

## 3. Data Protection & Security Controls
* **Storage Encryption**: Login sessions and authorization tokens are secured in AES-256 encrypted vaults via Android's `EncryptedSharedPreferences`.
* **Transport Encryption**: All communication channels with the Express.js server utilize Transport Layer Security (TLS 1.3/HTTPS).
* **Media Handling**: Captured damage photos are compressed down under 500KB inside local sandbox memory before uploading, and temp files are purged right after completion.

---

## 4. Retaining and Deleting Data
* Service request profiles and invoices are preserved as long as the user retains an active solar maintenance contract.
* Users can request profile closure and data erasure by contacting support.
