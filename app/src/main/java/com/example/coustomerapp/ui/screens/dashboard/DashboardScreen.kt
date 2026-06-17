package com.example.coustomerapp.ui.screens.dashboard

import com.example.coustomerapp.data.remote.dto.CurrentWeather
import com.example.coustomerapp.data.remote.dto.translateWeatherCode
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.coustomerapp.data.local.entities.CustomerProfileEntity
import com.example.coustomerapp.data.local.entities.DispatchRecordEntity
import com.example.coustomerapp.data.local.entities.InverterGenerationSummaryEntity
import com.example.coustomerapp.ui.components.ShimmerCard
import com.example.coustomerapp.ui.theme.*
import com.example.coustomerapp.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTracker: () -> Unit,
    onNavigateToDispatches: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val profile by viewModel.customerProfile.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val inverterSummary by viewModel.inverterSummary.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    // Weather state
    val weather by viewModel.weatherState.collectAsState()

    LaunchedEffect(profile?.city) {
        profile?.city?.let { city ->
            if (city.isNotBlank()) viewModel.loadWeather(city)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshDashboard() },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Greeting Header
            GreetingHeader(
                name = profile?.fullName ?: "User",
                onLogout = onLogout
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading && profile == null) {
                ShimmerDashboardContent()
            } else {
                // Hero Trajectory Banner → navigates to Installation Journey sub-page
                HeroTrajectoryBanner(
                    currentStage = profile?.projectStage ?: 0,
                    onTrackerClick = onNavigateToTracker
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Energy Generation Visualization (Canvas)
                EnergyVisualizationCard(
                    inverterSummary = inverterSummary,
                    systemSize = profile?.systemSizeKw ?: 0.0,
                    onClick = onNavigateToAnalytics
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Solar Metrics Grid
                profile?.let {
                    SolarMetricsGrid(
                        inverterSummary = inverterSummary,
                        profile = it
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weather Widget Card (replaces Weather Analytics Card)
                weather?.let { w ->
                    WeatherWidgetCard(weather = w, city = profile?.city ?: "Solar Site")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AMC & Maintenance Summary Card
                profile?.let {
                    AmcSummaryCard(profile = it)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Material Dispatches Quick View
                DashboardInfoCard(
                    title = "Material Dispatches",
                    icon = Icons.Default.Inventory,
                    accentColor = InfoAccent,
                    content = {
                        val dispatches by viewModel.dispatches.collectAsState()
                        if (dispatches.isEmpty()) {
                            Text("No recent dispatches", color = TextSecondary, fontSize = 14.sp)
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onNavigateToDispatches() }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        dispatches.first().itemName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text("Recent Dispatch", color = TextSecondary, fontSize = 12.sp)
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun WeatherWidgetCard(weather: CurrentWeather, city: String) {
    val (conditionText, _) = translateWeatherCode(weather.weatherCode)
    val weatherIcon = when (weather.weatherCode) {
        0 -> Icons.Default.WbSunny
        1, 2, 3 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.BlurOn
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Default.Grain
        71, 73, 75 -> Icons.Default.AcUnit
        95, 96, 99 -> Icons.Default.Thunderstorm
        else -> Icons.Default.Cloud
    }

    DashboardInfoCard(
        title      = "Local solar weather",
        icon       = Icons.Default.WbSunny,
        accentColor = InfoAccent,
        content    = {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(city, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Text("Condition: $conditionText", color = TextSecondary, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = conditionText,
                            tint = if (weather.weatherCode == 0) PrimarySolar else InfoAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${weather.temperature}°C", fontWeight = FontWeight.Black,
                            color = Color.White, fontSize = 18.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = "Wind speed",
                            tint = InfoAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${weather.windSpeed} km/h", fontWeight = FontWeight.Bold,
                            color = Color.White, fontSize = 13.sp)
                        Text("Wind speed", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    )
}

@Composable
fun HeroTrajectoryBanner(currentStage: Int, onTrackerClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(PrimarySolar, EcoAccent)
                )
            )
            .clickable { onTrackerClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Installation Journey",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                val percentage = (currentStage / 11f * 100).coerceIn(0f, 100f).toInt()
                Text(
                    "Phase: Step ${currentStage + 1} of 12 ($percentage%)",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Track", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun GreetingHeader(name: String, onLogout: () -> Unit) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = PrimarySolar,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = name.split(" ").firstOrNull() ?: "User",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        IconButton(
            onClick = onLogout,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(GlassWhite)
        ) {
            Icon(
                @Suppress("DEPRECATION")
                Icons.Default.Logout,
                contentDescription = "Logout",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ShimmerDashboardContent() {
    Column {
        ShimmerCard(height = 100.dp)
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerCard(height = 220.dp)
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerCard(height = 120.dp)
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerCard(height = 120.dp)
    }
}

@Composable
fun SolarMetricsGrid(inverterSummary: InverterGenerationSummaryEntity?, profile: CustomerProfileEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Daily Generation Card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassWhite)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Daily Output", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${inverterSummary?.dailyGeneration ?: 0.0} kWh",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        // Lifetime Generation Card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassWhite)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Lifetime", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${inverterSummary?.totalGeneration ?: 0.0} kWh",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        // Active Status
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassWhite)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Sync Health", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                val isOnline = inverterSummary?.status?.lowercase() == "online"
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    fontWeight = FontWeight.Bold,
                    color = if (isOnline) EcoAccent else ErrorAccent,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun AmcSummaryCard(profile: CustomerProfileEntity) {
    val totalVisits = profile.completedVisits + profile.pendingVisits
    val completionPercentage = if (totalVisits > 0) {
        profile.completedVisits.toFloat() / totalVisits
    } else {
        0f
    }

    DashboardInfoCard(
        title = "AMC & Maintenance",
        icon = Icons.Default.Build,
        accentColor = PrimarySolar,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("AMC Status", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    val amcActive = profile.amcStatus.uppercase() == "ACTIVE"
                    Text(
                        text = if (amcActive) "Active" else "Expired",
                        color = if (amcActive) EcoAccent else ErrorAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${profile.completedVisits} completed / ${profile.pendingVisits} pending",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Circular Progress Indicator for cleanings completion
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { completionPercentage },
                        modifier = Modifier.size(54.dp),
                        color = EcoAccent,
                        strokeWidth = 6.dp,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                    Text(
                        text = "${(completionPercentage * 100).toInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    )
}

@Composable
fun EnergyVisualizationCard(
    inverterSummary: InverterGenerationSummaryEntity?,
    systemSize: Double,
    onClick: () -> Unit
) {
    val currentPower = inverterSummary?.currentPower?.toFloat() ?: 0f
    val isOnline = inverterSummary?.status?.lowercase() == "online"

    var targetGeneration by remember { mutableStateOf(0f) }
    val animatedGeneration by animateFloatAsState(
        targetValue = targetGeneration,
        animationSpec = tween(durationMillis = 2000),
        label = "generation"
    )

    val peakPower = if (systemSize > 0) systemSize.toFloat() else 10f

    LaunchedEffect(currentPower) {
        delay(500)
        targetGeneration = currentPower
    }

    // Glowing solar pulse animation setup
    val infiniteTransition = rememberInfiniteTransition(label = "solar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    GlassCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Live Energy Generation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(28.dp))

            Box(contentAlignment = Alignment.Center) {
                if (isOnline && currentPower > 0) {
                    // Pulsing Ring under the progress arc
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(CircleShape)
                            .border(
                                width = 4.dp,
                                color = EcoAccent.copy(alpha = pulseAlpha),
                                shape = CircleShape
                            )
                    )
                }

                Canvas(modifier = Modifier.size(160.dp)) {
                    // Background arc
                    drawArc(
                        color = Color.White.copy(alpha = 0.06f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Foreground active arc
                    val sweepAngle = (animatedGeneration / peakPower) * 270f
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(PrimarySolar, GradientEnd, PrimarySolar)
                        ),
                        startAngle = 135f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.1f", animatedGeneration),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "of ${inverterSummary?.peakPower ?: "--"} kW Capacity",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(label = "Peak Cap", value = "${peakPower} kW", color = PrimarySolar)
                val efficiency = if (peakPower > 0) ((currentPower / peakPower) * 100).toInt() else 0
                StatChip(label = "Efficiency", value = "$efficiency%", color = EcoAccent)
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("$label: ", fontSize = 11.sp, color = TextSecondary)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
fun DashboardInfoCard(
    title: String,
    icon: ImageVector,
    accentColor: Color = PrimarySolar,
    content: @Composable () -> Unit
) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
