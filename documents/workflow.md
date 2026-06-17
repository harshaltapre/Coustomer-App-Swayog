# Workflows & Data Flows - SWAYOG Customer Mobile App

## 1. App Launch & Telemetry Loading Flow
This workflow describes how the app boots, instantly loads cached telemetry from Room database to present a fast UI, and issues background network calls to update the cache.

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant App as Jetpack Compose App
    participant LocalDB as Room Local SQLite
    participant Interceptor as Auth Interceptor
    participant Backend as Express.js Backend API
    participant Growatt as Growatt ShineServer API

    Customer->>App: Launch Application
    App->>LocalDB: Read cached Profile & Session details
    LocalDB-->>App: Return Profile (User ID: 105)
    App->>App: Display Cached Metrics immediately

    rect rgb(25, 30, 45)
        Note over App, Backend: Sync telemetry details in background
        App->>Interceptor: Request GET /inverter-generation
        Interceptor->>Interceptor: Inject Bearer <accessToken>
        Interceptor->>Backend: Forward Authorized Request
        alt Token Expired (401 Unauthorized Response)
            Backend-->>Interceptor: Return HTTP 401 Unauthorized
            Interceptor->>Interceptor: Intercept & Pause active pipelines
            Interceptor->>Backend: POST /auth/refresh with refreshToken
            Backend-->>Interceptor: Return New JWT access token
            Interceptor->>App: Update Session tokens in EncryptedPrefs
            Interceptor->>Backend: Retry original GET /inverter-generation with new Token
        end
        Backend->>Growatt: Request live solar register readings
        Growatt-->>Backend: Return power (kW) & yield (kWh) DTO
        Backend-->>App: HTTP 200 Return generation telemetry
    end

    App->>LocalDB: Update InverterGenerationSummaryEntity
    LocalDB-->>App: Reactive StateFlow trigger UI update
    App->>Customer: Render updated telemetry & Online badge
```

---

## 2. Offline Writing & Background Reconcile Flow
This diagram details the write-queue process that guarantees service ticket submissions are reconciled with backend nodes even when starting offline.

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant UI as Jetpack Compose UI Screen
    participant DB as Room Local DB Cache
    participant WM as WorkManager Queue
    participant Worker as ServiceRequestSyncWorker
    participant Server as Remote Express.js Server

    Customer->>UI: Submit Service Request (Cleaning)
    UI->>UI: Compress attached photos to JPEG <500KB
    UI->>DB: Save ticket in Local Database (isSynced = false)
    DB-->>UI: Instantly render ticket in UI list
    UI->>WM: Enqueue ServiceRequestSyncWorker (Constraint: Network.CONNECTED)

    alt Phone Offline
        WM-->>WM: Hold execution queue, poll network state monitors
    else Phone Online
        WM->>Worker: Trigger Worker Execution doWork()
        Worker->>DB: Read unsynced tickets (isSynced = false)
        DB-->>Worker: Return ticket fields & photo path
        Worker->>Server: POST Multipart form-data to /api/v1/customer/requests
        Server-->>Worker: Return HTTP 201 Created & DB ID
        Worker->>DB: Update ticket status (isSynced = true, id = server_id)
        DB-->>UI: Reactive UI redraw (Sync status indicator removes "unsynced" badge)
    end
```
