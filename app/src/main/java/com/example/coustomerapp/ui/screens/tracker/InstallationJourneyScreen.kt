package com.example.coustomerapp.ui.screens.tracker

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.coustomerapp.data.local.entities.InstallationStep
import com.example.coustomerapp.data.local.entities.StepStatus
import com.example.coustomerapp.ui.theme.*
import com.example.coustomerapp.ui.viewmodel.InstallationJourneyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationJourneyScreen(
    onNavigateBack: () -> Unit,
    viewModel: InstallationJourneyViewModel = hiltViewModel()
) {
    val steps by viewModel.steps.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val sheetState = rememberModalBottomSheetState()
    var selectedStep by remember { mutableStateOf<InstallationStep?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Installation Journey",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshTracker() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && steps.isEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    items(5) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassWhite)
                        )
                    }
                }
            } else {
                val completedCount = steps.count { it.status == StepStatus.COMPLETED }
                val totalCount = steps.size

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(GlassWhite)
                                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Progress",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        "$completedCount of $totalCount Steps",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(PrimarySolar, GradientEnd)
                                            )
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "${if (totalCount > 0) ((completedCount.toFloat() / totalCount) * 100).toInt() else 0}%",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(steps) { index, step ->
                        JourneyTimelineItem(
                            step = step,
                            isLast = index == steps.size - 1,
                            onClick = {
                                selectedStep = step
                                showBottomSheet = true
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }

        if (showBottomSheet && selectedStep != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = SurfaceDark
            ) {
                JourneyStepDetailsContent(step = selectedStep!!)
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun JourneyTimelineItem(
    step: InstallationStep,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (step.status) {
                            StepStatus.COMPLETED -> Brush.linearGradient(
                                colors = listOf(EcoAccent, EcoAccentLight)
                            )
                            StepStatus.IN_PROGRESS -> Brush.linearGradient(
                                colors = listOf(PrimarySolar, GradientEnd)
                            )
                            StepStatus.PENDING -> Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.04f)
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (step.status) {
                    StepStatus.COMPLETED -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    StepStatus.IN_PROGRESS -> Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    StepStatus.PENDING -> Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(
                            if (step.status == StepStatus.COMPLETED)
                                EcoAccent.copy(alpha = 0.4f)
                            else
                                Color.White.copy(alpha = 0.08f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassWhite)
                .border(
                    width = 1.dp,
                    color = when (step.status) {
                        StepStatus.IN_PROGRESS -> PrimarySolar.copy(alpha = 0.3f)
                        StepStatus.COMPLETED -> EcoAccent.copy(alpha = 0.15f)
                        else -> GlassBorder
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Text(
                text = step.title,
                fontWeight = FontWeight.Bold,
                color = if (step.status == StepStatus.PENDING) TextTertiary else Color.White,
                fontSize = 15.sp
            )
            if (step.date != null) {
                Text(
                    text = step.date,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (step.status == StepStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimarySolar.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Currently in progress",
                        color = PrimarySolar,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun JourneyStepDetailsContent(step: InstallationStep) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "Step ${step.id}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Status chip
        val statusColor = when (step.status) {
            StepStatus.COMPLETED -> EcoAccent
            StepStatus.IN_PROGRESS -> PrimarySolar
            else -> TextTertiary
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = step.status.name.replace("_", " "),
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Description",
            color = TextSecondary,
            fontSize = 13.sp
        )
        Text(
            text = step.description,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Target Schedules Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Target Start", color = TextSecondary, fontSize = 12.sp)
                Text(step.targetStartDate ?: "Pending", color = Color.White, fontWeight = FontWeight.Medium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Target End", color = TextSecondary, fontSize = 12.sp)
                Text(step.targetEndDate ?: "Pending", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }

        if (!step.inspectorNotes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Inspector Technical Notes", color = TextSecondary, fontSize = 12.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(step.inspectorNotes, color = Color.White, fontSize = 13.sp)
            }
        }

        if (step.documents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Verification Files", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            step.documents.forEach { doc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimarySolar.copy(alpha = 0.1f))
                        .border(1.dp, PrimarySolar.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .clickable {
                            Toast.makeText(context, "Opening $doc in Viewer", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = PrimarySolar,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(doc, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("View File", color = PrimarySolar, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
