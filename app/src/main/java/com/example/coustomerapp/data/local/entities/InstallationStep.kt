package com.example.coustomerapp.data.local.entities

data class InstallationStep(
    val id: Int,
    val title: String,
    val description: String,
    val status: StepStatus, // COMPLETED, IN_PROGRESS, PENDING
    val date: String? = null,
    val targetStartDate: String? = null,
    val targetEndDate: String? = null,
    val inspectorNotes: String? = null,
    val documents: List<String> = emptyList()
)

enum class StepStatus {
    COMPLETED, IN_PROGRESS, PENDING
}

val installationPhases = listOf(
    "Site Survey",
    "Document Collection",
    "Approval and Advance Payment",
    "Licensing",
    "2nd Instalment",
    "Procurement",
    "Vendor Selection",
    "Installation",
    "WCR (Work Completion Report)",
    "3rd Instalment",
    "Meter Installation & Subsidy Redeem",
    "System Handover"
)
