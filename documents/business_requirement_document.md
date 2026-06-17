# Business Requirement Document (BRD) - SWAYOG Customer Mobile App

## 1. Document Control
* **Project**: SWAYOG Customer Mobile Application
* **Version**: 1.0.0
* **Date**: 2026-06-15
* **Author**: Antigravity Developer

---

## 2. Business Objectives & Opportunity
Solar array owners face a major lack of visibility regarding their post-installation performance and customer service lifecycles. Once a physical solar installation is completed, users struggle to access live inverter logs, request panel cleaning, track shipping dispatches, and review financial ledgers.
The **SWAYOG Customer Mobile App** serves to bridge this gap. By bringing real-time telemetry analytics, billing ledgers, and maintenance requests to a native Android app, SWAYOG can increase client satisfaction, automate service delivery, and reduce overhead from support phone channels.

---

## 3. Scope of the Application
The application will focus on the customer lifecycle post-onboarding. Key system elements include:
* **Real-time Telemetry Dashboard**: Displaying power generation curves (kW) and cumulative output registers (kWh).
* **Self-Service Support Portal**: Ticket logging, location mapping, and maintenance booking.
* **Collections Interface**: Outstanding invoices ledgers linked to Razorpay SDK payments.

---

## 4. Key Business Requirements (KBR)

| ID | Business Requirement | Description | Impact / Priority |
|---|---|---|---|
| **KBR-001** | Inverter Generation Visibility | Real-time and historical telemetry data must be displayed to the user via clear visual charts (hourly, weekly, monthly, yearly). | **Critical** |
| **KBR-002** | Automatic AMC Cleanings | Cleanings and AMC maintenance visits must be bookable in-app to reduce call-center load. | **High** |
| **KBR-003** | Digital Payments | Customers must be able to view ledgers and pay outstanding invoices directly via standard card, UPI, or netbanking. | **Critical** |
| **KBR-004** | Offline Resiliency | Support tickets logged offline must sync automatically when the phone reconnects to the network. | **Medium** |
| **KBR-005** | Site Weather Context | Display current local weather conditions mapping to solar location coordinates. | **Medium** |

---

## 5. Success Metrics & KPIs
* **DAU Monitoring**: At least 60% of active solar array owners checking the telemetry daily.
* **Support Ticket Self-Service**: Shifting 45% of support calls to in-app ticket logs.
* **Outstanding AR Turnaround**: Reducing billing collections duration to under 48 hours via automated Razorpay notifications.
* **Sync Health Rate**: 99.8% successful database reconciliation rate for offline records.
