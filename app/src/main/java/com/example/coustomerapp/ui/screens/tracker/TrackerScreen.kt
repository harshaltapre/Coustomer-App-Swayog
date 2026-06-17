package com.example.coustomerapp.ui.screens.tracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.coustomerapp.ui.screens.dashboard.GlassCard
import com.example.coustomerapp.ui.theme.*
import com.example.coustomerapp.ui.viewmodel.TrackerViewModel
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrackerViewModel = hiltViewModel()
) {
    val selectedPeriod  by viewModel.selectedPeriod.collectAsState()
    val historyPoints   by viewModel.historyPoints.collectAsState()
    val summary         by viewModel.inverterSummary.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val isRefreshing    by viewModel.isRefreshing.collectAsState()
    val pullState       = rememberPullToRefreshState()

    var touchX          by remember { mutableStateOf<Float?>(null) }
    var activeIndex     by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedPeriod) { touchX = null; activeIndex = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Generation Telemetry", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh    = { viewModel.refreshTracker() },
            state        = pullState,
            modifier     = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(8.dp))

                // ── Period selector tabs ──
                val periods = listOf("realtime" to "Real-time", "weekly" to "Weekly",
                    "monthly" to "Monthly", "yearly" to "Yearly")
                val selectedIdx = periods.indexOfFirst { it.first == selectedPeriod }.coerceAtLeast(0)

                TabRow(
                    selectedTabIndex = selectedIdx,
                    containerColor   = SurfaceDark,
                    contentColor     = PrimarySolar,
                    indicator        = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedIdx]),
                                color = PrimarySolar
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                ) {
                    periods.forEach { (key, label) ->
                        Tab(
                            selected             = selectedPeriod == key,
                            onClick              = { viewModel.setPeriod(key) },
                            text                 = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                            selectedContentColor   = PrimarySolar,
                            unselectedContentColor = TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Tooltip info box ──
                if (activeIndex != null && activeIndex!! < historyPoints.size) {
                    val point    = historyPoints[activeIndex!!]
                    val valueStr = if (selectedPeriod == "realtime")
                        "${point.powerValue ?: 0.0} kW" else "${point.generationValue ?: 0.0} kWh"
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimarySolar.copy(alpha = 0.15f))
                            .border(1.dp, PrimarySolar.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Timeframe: ${point.label}  •  Yield: $valueStr",
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                        Text("Drag finger across graph to inspect values", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Chart Canvas ──
                GlassCard(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    if (historyPoints.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (isLoading) CircularProgressIndicator(color = PrimarySolar)
                            else Text("No telemetry data available", color = TextSecondary)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Canvas(
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                                    .pointerInput(historyPoints) {
                                        detectTapGestures(onPress = { offset -> touchX = offset.x })
                                    }
                                    .pointerInput(historyPoints) {
                                        detectDragGestures(
                                            onDragEnd    = { touchX = null; activeIndex = null },
                                            onDragCancel = { touchX = null; activeIndex = null },
                                            onDrag       = { change, _ -> touchX = change.position.x }
                                        )
                                    }
                            ) {
                                val w           = size.width
                                val h           = size.height
                                val padLeft     = 42.dp.toPx()
                                val padRight    = 16.dp.toPx()
                                val padTop      = 20.dp.toPx()
                                val padBottom   = 36.dp.toPx()
                                val graphW      = w - padLeft - padRight
                                val graphH      = h - padTop - padBottom
                                val maxVal      = historyPoints.maxOfOrNull {
                                    if (selectedPeriod == "realtime") it.powerValue ?: 1.0 else it.generationValue ?: 1.0
                                }?.coerceAtLeast(1.0) ?: 1.0
                                val stepX       = if (historyPoints.size > 1) graphW / (historyPoints.size - 1) else graphW

                                touchX?.let { tx ->
                                    activeIndex = ((tx - padLeft) / stepX).roundToInt().coerceIn(0, historyPoints.size - 1)
                                }

                                // ── Draw Horizontal Grid Lines and Y-Axis Labels ──
                                val gridLines = 4
                                val textPaint = Paint().apply {
                                    color = android.graphics.Color.parseColor("#94A3B8") // TextSecondary
                                    textSize = 9.dp.toPx()
                                    textAlign = Paint.Align.RIGHT
                                    isAntiAlias = true
                                }
                                for (i in 0..gridLines) {
                                    val fraction = i.toFloat() / gridLines
                                    val y = h - padBottom - fraction * graphH
                                    // Grid line
                                    drawLine(
                                        color = if (i == 0) GlassBorder else Color.White.copy(alpha = 0.04f),
                                        start = Offset(padLeft, y),
                                        end = Offset(w - padRight, y),
                                        strokeWidth = if (i == 0) 2f else 1f
                                    )
                                    // Y value label
                                    val unit = if (selectedPeriod == "realtime") "kW" else "kWh"
                                    val yVal = fraction * maxVal
                                    val label = String.format("%.1f", yVal)
                                    drawContext.canvas.nativeCanvas.drawText(
                                        "$label $unit",
                                        padLeft - 8.dp.toPx(),
                                        y + 3.dp.toPx(),
                                        textPaint
                                    )
                                }

                                // ── Draw Graph Data ──
                                if (selectedPeriod == "realtime") {
                                    // ── Spline area chart ──
                                    val path     = Path()
                                    val fillPath = Path()
                                    historyPoints.forEachIndexed { idx, pt ->
                                        val valY = pt.powerValue ?: 0.0
                                        val ptX  = padLeft + idx * stepX
                                        val ptY  = h - padBottom - (valY / maxVal).toFloat() * graphH
                                        if (idx == 0) {
                                            path.moveTo(ptX, ptY)
                                            fillPath.moveTo(ptX, h - padBottom)
                                            fillPath.lineTo(ptX, ptY)
                                        } else {
                                            val prevX  = padLeft + (idx - 1) * stepX
                                            val prevY  = h - padBottom - ((historyPoints[idx - 1].powerValue ?: 0.0) / maxVal).toFloat() * graphH
                                            val ctrlX1 = (prevX + ptX) / 2f
                                            path.cubicTo(ctrlX1, prevY, ctrlX1, ptY, ptX, ptY)
                                            fillPath.cubicTo(ctrlX1, prevY, ctrlX1, ptY, ptX, ptY)
                                        }
                                        if (idx == historyPoints.size - 1) { 
                                            fillPath.lineTo(ptX, h - padBottom)
                                            fillPath.close() 
                                        }
                                    }
                                    drawPath(fillPath, Brush.verticalGradient(listOf(EcoAccent.copy(alpha = 0.35f), Color.Transparent)))
                                    drawPath(path, EcoAccent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                                } else {
                                    // ── Rounded gradient bar chart ──
                                    val barW = (stepX * 0.45f).coerceIn(8.dp.toPx(), 32.dp.toPx())
                                    historyPoints.forEachIndexed { idx, pt ->
                                        val valY    = pt.generationValue ?: 0.0
                                        val ptX     = padLeft + idx * stepX - barW / 2f
                                        val ptY     = h - padBottom - (valY / maxVal).toFloat() * graphH
                                        val barH    = (valY / maxVal).toFloat() * graphH
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(listOf(InfoAccentLight, InfoAccent)),
                                            topLeft = Offset(ptX, ptY),
                                            size = Size(barW, barH.coerceAtLeast(2f)),
                                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                        )
                                    }
                                }

                                // ── Draw X-Axis Labels ──
                                val labelPaint = Paint().apply {
                                    color = android.graphics.Color.parseColor("#64748B") // TextTertiary
                                    textSize = 8.5.dp.toPx()
                                    textAlign = Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                val step = (historyPoints.size / 5).coerceAtLeast(1)
                                historyPoints.forEachIndexed { idx, pt ->
                                    val ptX = padLeft + idx * stepX
                                    if (idx % step == 0 || idx == historyPoints.size - 1) {
                                        drawContext.canvas.nativeCanvas.drawText(
                                            pt.label,
                                            ptX,
                                            h - padBottom + 18.dp.toPx(),
                                            labelPaint
                                        )
                                    }
                                }

                                // ── Interactive tooltip cursor ──
                                activeIndex?.let { idx ->
                                    val ptX  = padLeft + idx * stepX
                                    val yVal = (if (selectedPeriod == "realtime") historyPoints[idx].powerValue else historyPoints[idx].generationValue) ?: 0.0
                                    val ptY  = h - padBottom - (yVal / maxVal).toFloat() * graphH
                                    drawLine(Color.White.copy(alpha = 0.5f), Offset(ptX, padTop), Offset(ptX, h - padBottom), strokeWidth = 1.dp.toPx())
                                    drawCircle(PrimarySolar, radius = 6.dp.toPx(), center = Offset(ptX, ptY))
                                    drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(ptX, ptY))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Stats grid ──
                Text("Analytics summary", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Triple(Icons.Default.Speed,      PrimarySolar, "Today's Output" to "${summary?.dailyGeneration ?: 0.0} kWh"),
                        Triple(Icons.Default.Leaderboard, InfoAccent,  "Peak Output"    to "${summary?.peakPower ?: 0.0} kW"),
                        Triple(Icons.Default.Sync,        EcoAccent,   "Sync Status"    to if (summary?.isSimulated == true) "Simulated" else "Live Cloud")
                    ).forEach { (icon, tint, label) ->
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassWhite)
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(label.first,  color = TextSecondary, fontSize = 11.sp)
                                Text(label.second, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(30.dp))
            }
        }
    }
}
