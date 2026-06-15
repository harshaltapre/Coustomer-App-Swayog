package com.example.coustomerapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.local.entities.InstallationStep
import com.example.coustomerapp.data.local.entities.StepStatus
import com.example.coustomerapp.data.local.entities.installationPhases
import com.example.coustomerapp.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstallationJourneyViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    companion object {
        private const val TAG = "InstallationJourneyVM"
    }

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine error", throwable)
        _isLoading.value = false
        _isRefreshing.value = false
    }

    private val _steps = MutableStateFlow<List<InstallationStep>>(emptyList())
    val steps: StateFlow<List<InstallationStep>> = _steps.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        initialLoad()
        observeProfile()
    }

    private fun initialLoad() {
        viewModelScope.launch(errorHandler) {
            _isLoading.value = true
            try {
                repository.refreshProfile()
                val profile = repository.customerProfile.first()
                updateSteps(profile?.projectStage ?: 0)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun observeProfile() {
        viewModelScope.launch(errorHandler) {
            repository.customerProfile.collect { profile ->
                updateSteps(profile?.projectStage ?: 0)
            }
        }
    }

    private fun updateSteps(projectStage: Int) {
        _steps.value = installationPhases.mapIndexed { index, title ->
            val status = when {
                index <= projectStage -> StepStatus.COMPLETED
                index == projectStage + 1 -> StepStatus.IN_PROGRESS
                else -> StepStatus.PENDING
            }

            val dateStr = if (index <= projectStage) "Completed on ${10 + index} May 2026" else null
            val startTarget = "${index + 1} May 2026"
            val endTarget = "${index + 3} May 2026"

            InstallationStep(
                id = index + 1,
                title = title,
                description = getStepDescription(title),
                status = status,
                date = dateStr,
                targetStartDate = startTarget,
                targetEndDate = endTarget,
                inspectorNotes = getInspectorNotes(title),
                documents = getDocuments(title)
            )
        }
    }

    private fun getStepDescription(title: String): String {
        return when (title) {
            "Site Survey" -> "Our team visits your location to assess the site for solar installation."
            "Document Collection" -> "Gathering all necessary documents including ID proof, electricity bills, and property papers."
            "Approval and Advance Payment" -> "Project approval and initial advance payment processing."
            "Licensing" -> "Obtaining necessary licenses and permissions from authorities."
            "2nd Instalment" -> "Processing the second instalment payment."
            "Procurement" -> "Procuring solar panels, inverters, and other equipment."
            "Vendor Selection" -> "Selecting the best vendors for installation."
            "Installation" -> "Physical installation of solar panels and equipment at your site."
            "WCR (Work Completion Report)" -> "Generating the official Work Completion Report."
            "3rd Instalment" -> "Processing the final instalment payment."
            "Meter Installation & Subsidy Redeem" -> "Installing the net meter and processing government subsidy claims."
            "System Handover" -> "Final handover of the complete solar system to you."
            else -> "Details for $title"
        }
    }

    private fun getInspectorNotes(title: String): String {
        return when (title) {
            "Site Survey" -> "Site survey completed, structural integrity passed. Roof slope optimized at 23 degrees facing south."
            "Document Collection" -> "Aadhaar, Electricity Bill, and property tax receipt verified. Everything is compliant."
            "Approval and Advance Payment" -> "Advance payment of ₹50,000 received. Solar net metering application approved by DISCOM."
            "Licensing" -> "NOC obtained from municipal corporation and local electrical inspector office."
            "2nd Instalment" -> "Second instalment invoice raised and fully verified."
            "Procurement" -> "Growatt inverter and Mono-PERC solar modules procured from central warehouse and packaged."
            "Vendor Selection" -> "SWAYOG Certified vendor selected for structural layout mounting and load-bearing test."
            "Installation" -> "Solar structure mounted. Panel cabling routed through standard PVC conduits. Grounding complete."
            "WCR (Work Completion Report)" -> "Work completion report signed by certified electrical engineer."
            "3rd Instalment" -> "Final balance payment verified."
            "Meter Installation & Subsidy Redeem" -> "Net-meter testing successful. Subsidy application dispatched."
            "System Handover" -> "System commissioned. Handover toolkit with generation monitoring app access credentials shared."
            else -> "All inspector safety approvals verified."
        }
    }

    private fun getDocuments(title: String): List<String> {
        return when (title) {
            "Site Survey" -> listOf("site_survey_layout.pdf", "roof_structural_draft.dwg")
            "Document Collection" -> listOf("utility_bill_receipt.pdf", "tax_noc.pdf")
            "Approval and Advance Payment" -> listOf("advance_receipt_50k.pdf")
            "Licensing" -> listOf("discom_licence_approved.pdf")
            "2nd Instalment" -> listOf("invoice_instalment_2.pdf")
            "WCR (Work Completion Report)" -> listOf("work_completion_report.pdf", "electrical_wiring_schematic.pdf")
            "System Handover" -> listOf("handover_certificate.pdf", "warranty_card.pdf")
            else -> emptyList()
        }
    }

    fun refreshTracker() {
        viewModelScope.launch(errorHandler) {
            _isRefreshing.value = true
            try {
                repository.refreshProfile()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
