package com.example.coustomerapp.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.coustomerapp.data.local.entities.InverterGenerationHistoryEntity
import com.example.coustomerapp.ui.screens.dashboard.GlassCard
import com.example.coustomerapp.ui.theme.*
import com.example.coustomerapp.ui.viewmodel.AnalyticsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val historyPoints by viewModel.historyPoints.collectAsState()
    val summary by viewModel.inverterSummary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var touchX by remember { mutableStateOf<Float?>(null) }
    var activeIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedPeriod) {
        touchX = null
        activeIndex = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Energy Analytics",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Period Selection Tabs
            val periods = listOf(
                "realtime" to "Real-time",
                "daily" to "Daily",
                "monthly" to "Monthly",
                "yearly" to "Yearly"
            )
            TabRow(
                selectedTabIndex = periods.indexOfFirst { it.first == selectedPeriod }.coerceAtLeast(0),
                containerColor = SurfaceDark,
                contentColor = PrimarySolar,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[periods.indexOfFirst { it.first == selectedPeriod }.coerceAtLeast(0)]),
                            color = PrimarySolar
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                periods.forEach { (key, label) ->
                    Tab(
                        selected = selectedPeriod == key,
                        onClick = { viewModel.setPeriod(key) },
                        text = {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        selectedContentColor = PrimarySolar,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tooltip indicator card if active
            if (activeIndex != null && activeIndex!! < historyPoints.size) {
                val point = historyPoints[activeIndex!!]
                val valueStr = if (selectedPeriod == "realtime") {
                    "${point.powerValue ?: 0.0} kW"
                } else {
                    "${point.generationValue ?: 0.0} kWh"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimarySolar.copy(alpha = 0.15f))
                        .border(1.dp, PrimarySolar.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Timeframe: ${point.label}  •  Output: $valueStr",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Chart Canvas Panel
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                if (historyPoints.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = PrimarySolar)
                        } else {
                            Text("No telemetry data for this period", color = TextSecondary)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Drawing Area
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .pointerInput(historyPoints) {
                                    detectTapGestures(
                                        onPress = { offset ->
                                            touchX = offset.x
                                        }
                                    )
                                }
                                .pointerInput(historyPoints) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            touchX = null
                                            activeIndex = null
                                        },
                                        onDragCancel = {
                                            touchX = null
                                            activeIndex = null
                                        },
                                        onDrag = { change, dragAmount ->
                                            touchX = change.position.x
                                        }
                                    )
                                }
                        ) {
                            val width = size.width
                            val height = size.height
                            val padding = 36.dp.toPx()
                            val graphWidth = width - padding * 2
                            val graphHeight = height - padding * 2

                            // Max value for scaling
                            val maxVal = historyPoints.maxOfOrNull {
                                if (selectedPeriod == "realtime") it.powerValue ?: 1.0
                                else it.generationValue ?: 1.0
                            }?.coerceAtLeast(1.0) ?: 1.0

                            val stepX = if (historyPoints.size > 1) graphWidth / (historyPoints.size - 1) else graphWidth

                            // Find active touch index
                            touchX?.let { tx ->
                                val relativeX = tx - padding
                                val index = (relativeX / stepX).roundToInt().coerceIn(0, historyPoints.size - 1)
                                activeIndex = index
                            }

                            // Draw X & Y axis lines
                            drawLine(
                                color = GlassBorder,
                                start = Offset(padding, height - padding),
                                end = Offset(width - padding, height - padding),
                                strokeWidth = 2f
                            )

                            // 1. Draw Real-time spline area chart
                            if (selectedPeriod == "realtime") {
                                val path = Path()
                                val fillPath = Path()

                                historyPoints.forEachIndexed { idx, pt ->
                                    val valY = pt.powerValue ?: 0.0
                                    val ptX = padding + idx * stepX
                                    val ptY = height - padding - (valY / maxVal).toFloat() * graphHeight

                                    if (idx == 0) {
                                        path.moveTo(ptX, ptY)
                                        fillPath.moveTo(ptX, height - padding)
                                        fillPath.lineTo(ptX, ptY)
                                    } else {
                                        val prevPtX = padding + (idx - 1) * stepX
                                        val prevPtY = height - padding - ((historyPoints[idx - 1].powerValue ?: 0.0) / maxVal).toFloat() * graphHeight
                                        
                                        // Cubic Bezier calculation
                                        val ctrlX1 = (prevPtX + ptX) / 2f
                                        val ctrlY1 = prevPtY
                                        val ctrlX2 = (prevPtX + ptX) / 2f
                                        val ctrlY2 = ptY

                                        path.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, ptX, ptY)
                                        fillPath.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, ptX, ptY)
                                    }

                                    if (idx == historyPoints.size - 1) {
                                        fillPath.lineTo(ptX, height - padding)
                                        fillPath.close()
                                    }
                                }

                                // Draw Area Gradient
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(EcoAccent.copy(alpha = 0.35f), Color.Transparent)
                                    )
                                )

                                // Draw Spline Stroke
                                drawPath(
                                    path = path,
                                    color = EcoAccent,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            } else {
                                // 2. Draw Historical rounded bar charts
                                val barWidth = (stepX * 0.5f).coerceIn(8.dp.toPx(), 40.dp.toPx())
                                historyPoints.forEachIndexed { idx, pt ->
                                    val valY = pt.generationValue ?: 0.0
                                    val ptX = padding + idx * stepX - barWidth / 2f
                                    val ptY = height - padding - (valY / maxVal).toFloat() * graphHeight
                                    val barHeight = (valY / maxVal).toFloat() * graphHeight

                                    drawRoundRect(
                                        color = InfoAccent,
                                        topLeft = Offset(ptX, ptY),
                                        size = Size(barWidth, barHeight.coerceAtLeast(2f)),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                            }

                            // Draw X Labels
                            historyPoints.forEachIndexed { idx, pt ->
                                val ptX = padding + idx * stepX
                                if (idx % (historyPoints.size / 5).coerceAtLeast(1) == 0 || idx == historyPoints.size - 1) {
                                    // Simple dot for labels
                                    drawCircle(
                                        color = GlassBorder,
                                        radius = 3f,
                                        center = Offset(ptX, height - padding)
                                    )
                                }
                            }

                            // Draw Tooltip line
                            activeIndex?.let { idx ->
                                val ptX = padding + idx * stepX
                                drawLine(
                                    color = Color.White.copy(alpha = 0.6f),
                                    start = Offset(ptX, padding),
                                    end = Offset(ptX, height - padding),
                                    strokeWidth = 1.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawCircle(
                                    color = PrimarySolar,
                                    radius = 6.dp.toPx(),
                                    center = Offset(ptX, height - padding - (((if (selectedPeriod == "realtime") historyPoints[idx].powerValue else historyPoints[idx].generationValue) ?: 0.0) / maxVal).toFloat() * graphHeight)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metadata summary grid
            Text(
                "Telemetry Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Yield Metric Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassWhite)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = PrimarySolar, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Yield Metric", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "${summary?.dailyGeneration ?: 0.0} kWh",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }

                // Peak Power Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassWhite)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(Icons.Default.Leaderboard, contentDescription = null, tint = InfoAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Peak Output", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "${summary?.peakPower ?: 0.0} kW",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }

                // Sync Monitor Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassWhite)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = EcoAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sync Monitor", color = TextSecondary, fontSize = 11.sp)
                        val isSimulated = summary?.isSimulated ?: false
                        Text(
                            text = if (isSimulated) "Simulated" else "Live Cloud",
                            fontWeight = FontWeight.Bold,
                            color = if (isSimulated) WarningAccent else EcoAccent,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
