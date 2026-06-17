# API Documentation - SWAYOG Customer Mobile App

## 1. Authentication Endpoints

### 1.1 User Login
Authenticates user login credentials.
* **HTTP Method**: `POST`
* **Route**: `/api/v1/auth/login`
* **Request Header**: `Content-Type: application/json`
* **Request Payload**:
```json
{
  "loginId": "customer_harshal",
  "password": "CustomerPassword123"
}
```
* **Response Payload (200 OK - Success)**:
```json
{
  "status": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "48ca83d5-e9df-41a4-9b2b-9be24a3501fe"
  }
}
```

### 1.2 Access Token Refresh
Exchanges a long-lived refresh token for a short-lived access token.
* **HTTP Method**: `POST`
* **Route**: `/api/v1/auth/refresh`
* **Request Header**: `Content-Type: application/json`
* **Request Payload**:
```json
{
  "refreshToken": "48ca83d5-e9df-41a4-9b2b-9be24a3501fe"
}
```
* **Response Payload (200 OK - Success)**:
```json
{
  "status": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "23cf84d9-a2df-4223-bd21-1fe231bc90ff"
  }
}
```

---

## 2. Inverter Telemetry Endpoints

### 2.1 Fetch Real-time Telemetry Summary
Returns summary statistics of the client array's generation.
* **HTTP Method**: `GET`
* **Route**: `/api/v1/subadmin/customers/{customerId}/inverter-generation`
* **Request Header**: `Authorization: Bearer <accessToken>`
* **Response Payload (200 OK - Success)**:
```json
{
  "dailyGeneration": 12.45,
  "totalGeneration": 3450.8,
  "peakPower": 5.4,
  "currentPower": 2.1,
  "isSimulated": false,
  "status": "online",
  "lastUpdated": "2026-06-15T11:43:00Z"
}
```

### 2.2 Fetch Inverter Generation History
Returns arrays of data points based on query parameters.
* **HTTP Method**: `GET`
* **Route**: `/api/v1/subadmin/customers/{customerId}/inverter-generation-history`
* **Request Header**: `Authorization: Bearer <accessToken>`
* **Query Parameters**:
  * `period`: `realtime` | `daily` | `yearly`
* **Response Payload (200 OK - Success)**:
```json
{
  "customerId": 105,
  "period": "daily",
  "history": [
    { "label": "09 Jun", "power": null, "generation": 14.2 },
    { "label": "10 Jun", "power": null, "generation": 15.1 },
    { "label": "11 Jun", "power": null, "generation": 13.8 },
    { "label": "12 Jun", "power": null, "generation": 14.5 },
    { "label": "13 Jun", "power": null, "generation": 16.2 },
    { "label": "14 Jun", "power": null, "generation": 12.1 },
    { "label": "15 Jun", "power": null, "generation": 11.9 }
  ]
}
```

---

## 3. Service Ticket Endpoints

### 3.1 Create Service request
Logs a maintenance or panel cleaning request.
* **HTTP Method**: `POST`
* **Route**: `/api/v1/customer/requests`
* **Request Header**: `Authorization: Bearer <accessToken>`, `Content-Type: multipart/form-data`
* **Request Fields**:
  * `serviceType`: String (e.g. "Panel Cleaning")
  * `description`: String
  * `address`: String
  * `latitude`: Double
  * `longitude`: Double
  * `preferredDate`: String (Format: `yyyy-MM-dd`)
  * `images`: Binary File (Optional attachment)
* **Response Payload (201 Created - Success)**:
```json
{
  "status": "success",
  "data": {
    "id": 982,
    "serviceType": "Panel Cleaning",
    "status": "pending",
    "createdAt": "2026-06-15T11:44:00Z"
  }
}
```

---

## 4. Razorpay Payments Integration

### 4.1 Create Razorpay Payment Order
Initiates an outstanding ledger balance check and payment order.
* **HTTP Method**: `POST`
* **Route**: `/api/v1/customer/payments/razorpay/order`
* **Request Header**: `Authorization: Bearer <accessToken>`, `Content-Type: application/json`
* **Request Payload**:
```json
{
  "amountInRupees": 2500.0,
  "description": "Outstanding balance payment",
  "referenceId": "pay_1718451842000"
}
```
* **Response Payload (200 OK - Success)**:
```json
{
  "status": "success",
  "data": {
    "id": "order_EKcdx2Nh6K2G21",
    "amount": 250000,
    "currency": "INR"
  }
}
```

### 4.2 Verify Razorpay Signature Receipt
Validates the Razorpay gateway transaction signature.
* **HTTP Method**: `POST`
* **Route**: `/api/v1/customer/payments/razorpay/verify`
* **Request Header**: `Authorization: Bearer <accessToken>`, `Content-Type: application/json`
* **Request Payload**:
```json
{
  "razorpay_order_id": "order_EKcdx2Nh6K2G21",
  "razorpay_payment_id": "pay_G3x9Vd1z5Jd2aC",
  "razorpay_signature": "f2cd8c17b8f9e612ab78b671a9b240182ce..."
}
```
* **Response Payload (200 OK - Success)**:
```json
{
  "status": "success",
  "message": "Payment verified and ledger reconciled."
}
```
