# PROMPT FOR ANDROID STUDIO AI (GEMINI/COPILOT)

Use this step-by-step developer prompt to implement real-time inverter telemetry graphs, dynamic local weather conditions, solar output metrics, and sync health stats. It replaces the old static installation journey tracker with live solar performance analytics.

---

### **Overview of Changes to Implement**
1. **Home Screen (`DashboardScreen.kt` & `DashboardViewModel.kt`):**
   * **Remove:** `CarbonOffsetVisualizerCard` and the step progress calculations from the home page.
   * **Add:** A dynamic, glassmorphic **Live Weather Widget** showing temperature (°C), wind speed (km/h), and clouds/conditions (e.g., "Partly Cloudy", "Sunny", "Overcast", etc.) using the customer's profile `city` field and Open-Meteo's free weather API.
   * **Update Live Generation:** In `EnergyVisualizationCard`, render the current kW generation against the maximum system size capacity (e.g., `4.2 kW / 5.4 kW Capacity`) dynamically.
   * **Update Metrics Grid:** Provide clear options/metric boxes displaying:
     * **Daily Output (Today's Yield):** `${inverterSummary.dailyGeneration} kWh`
     * **Lifetime Output:** `${inverterSummary.totalGeneration} kWh`
     * **Sync Health:** Glowing Online/Offline sync status with latest sync timestamp.

2. **Tracking Screen (`TrackerScreen.kt` & `TrackerViewModel.kt`):**
   * **Remove:** The 12-stage installation timeline timeline and bottom sheet details.
   * **Replace with:** **Real-time & Period Telemetry Graphs Screen** (similar to the analytics module).
   * **Add selector tabs:** "Real-time" (Today), "Weekly" (Last 7 days), "Monthly" (Current Month), "Yearly" (Current Year).
   * **Graphs rendering:**
     * **Real-time:** Area Line Chart showing kW power output curve over time of day (Spline Bezier path + glowing green gradient fill).
     * **Weekly/Monthly/Yearly:** Vertical Bar Chart showing kWh generation over time with brand-blue rounded bars.
     * **Interactive Tooltip:** Add a drag/tap detector overlay rendering the precise timestamped yield details on touch.

---

### **Step 1: Define Weather APIs & DTOs**

Add the following structures to `app/src/main/java/com/example/coustomerapp/data/remote/dto/CustomerDtos.kt` to model the Open-Meteo geocoding and forecast payloads. These APIs are free and require no keys.

```kotlin
// Open-Meteo Geocoding Search payload mapping
data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?
)

// Open-Meteo Weather forecast payload mapping
data class WeatherResponse(
    val current: CurrentWeather?
)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("wind_speed_10m") val windSpeed: Double,
    @SerializedName("weather_code") val weatherCode: Int
)

// Helper to translate weather codes to human-readable text & icons
fun translateWeatherCode(code: Int): Pair<String, String> {
    return when (code) {
        0 -> "Sunny" to "☀️"
        1, 2, 3 -> "Partly Cloudy" to "🌤️"
        45, 48 -> "Foggy" to "🌫️"
        51, 53, 55 -> "Drizzle" to "🌦️"
        61, 63, 65 -> "Rainy" to "🌧️"
        71, 73, 75 -> "Snowy" to "❄️"
        80, 81, 82 -> "Rain Showers" to "🌧️"
        95, 96, 99 -> "Thunderstorm" to "⛈️"
        else -> "Cloudy" to "☁️"
    }
}
```

Add weather Retrofit methods in `app/src/main/java/com/example/coustomerapp/data/remote/ApiService.kt`. Annotate with full URLs to bypass the local backend URL prefix:

```kotlin
@GET("https://geocoding-api.open-meteo.com/v1/search")
suspend fun getCoordinatesByCity(
    @Query("name") cityName: String,
    @Query("count") count: Int = 1,
    @Query("language") language: String = "en",
    @Query("format") format: String = "json"
): Response<GeocodingResponse>

@GET("https://api.open-meteo.com/v1/forecast")
suspend fun getWeather(
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("current") current: String = "temperature_2m,wind_speed_10m,weather_code"
): Response<WeatherResponse>
```

---

### **Step 2: Add Weather Fetch Logic to CustomerRepository.kt**

Add the weather fetching implementation inside `app/src/main/java/com/example/coustomerapp/data/repository/CustomerRepository.kt`:

```kotlin
// In CustomerRepository.kt
suspend fun fetchWeatherForCity(cityName: String): CurrentWeather? {
    return try {
        val geoResponse = apiService.getCoordinatesByCity(cityName)
        if (geoResponse.isSuccessful) {
            val result = geoResponse.body()?.results?.firstOrNull()
            if (result != null) {
                val weatherRes = apiService.getWeather(result.latitude, result.longitude)
                if (weatherRes.isSuccessful) {
                    return weatherRes.body()?.current
                }
            }
        }
        null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
```

---

### **Step 3: Modify DashboardViewModel.kt to Expose Weather State**

Update `app/src/main/java/com/example/coustomerapp/ui/viewmodel/DashboardViewModel.kt` to load weather dynamically based on the customer's city field:

```kotlin
// Add this state to DashboardViewModel.kt
private val _weatherState = MutableStateFlow<CurrentWeather?>(null)
val weatherState: StateFlow<CurrentWeather?> = _weatherState.asStateFlow()

// Add weather fetch inside initialLoad / refresh logic
fun loadWeather(city: String) {
    viewModelScope.launch(errorHandler) {
        val weather = repository.fetchWeatherForCity(city)
        _weatherState.value = weather
    }
}

// Ensure you call loadWeather(profile.city) when the customer profile is collected or fetched
```

---

### **Step 4: Update the Dashboard Home UI (DashboardScreen.kt)**

In `app/src/main/java/com/example/coustomerapp/ui/screens/dashboard/DashboardScreen.kt`:
1. **Remove** the `CarbonOffsetVisualizerCard`.
2. **Add** the glassmorphic weather card using the coordinates/city:

```kotlin
// Replace CarbonOffsetVisualizerCard call with this Weather Card
val weather by viewModel.weatherState.collectAsState()
LaunchedEffect(profile?.city) {
    profile?.city?.let { city ->
        if (city.isNotBlank()) viewModel.loadWeather(city)
    }
}

weather?.let { w ->
    WeatherWidgetCard(weather = w, city = profile?.city ?: "Local Site")
}

@Composable
fun WeatherWidgetCard(weather: CurrentWeather, city: String) {
    val (conditionText, iconStr) = translateWeatherCode(weather.weatherCode)
    DashboardInfoCard(
        title = "Local Solar Weather",
        icon = Icons.Default.WbSunny,
        accentColor = InfoAccent,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = city,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Condition: $conditionText",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Weather icon & Temp display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(iconStr, fontSize = 28.sp)
                        Text(
                            text = "${weather.temperature}°C",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }

                    // Wind speed info
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💨", fontSize = 20.sp)
                        Text(
                            text = "${weather.windSpeed} km/h",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text("Wind Speed", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    )
}
```

3. **Modify** `EnergyVisualizationCard` to display live current power generation over peak generation capacity:

```kotlin
// Inside EnergyVisualizationCard Composable in DashboardScreen.kt
// Update the central circular canvas and Text block:
Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
        text = String.format("%.1f", animatedGeneration),
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Text(
        text = "of ${peakPower} kW Capacity",
        fontSize = 12.sp,
        color = TextSecondary
    )
}
```

4. **Update** `HeroTrajectoryBanner` title to "Solar Telemetry Dashboard" and subtext to "View real-time inverter production graphs & history".

---

### **Step 5: Replace TrackerScreen.kt and TrackerViewModel.kt with Interactive Graphs**

We will rewrite `TrackerScreen.kt` to render interactive tabs ("Real-time", "Weekly", "Monthly", "Yearly") containing the telemetry charts, tooltips, and sync credentials health.

#### **1. Replace `TrackerViewModel.kt` content:**

```kotlin
package com.example.coustomerapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coustomerapp.data.local.entities.InverterGenerationHistoryEntity
import com.example.coustomerapp.data.local.entities.InverterGenerationSummaryEntity
import com.example.coustomerapp.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("TrackerViewModel", "Coroutine Error", throwable)
        _isLoading.value = false
        _isRefreshing.value = false
    }

    private val _selectedPeriod = MutableStateFlow("realtime")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _historyPoints = MutableStateFlow<List<InverterGenerationHistoryEntity>>(emptyList())
    val historyPoints: StateFlow<List<InverterGenerationHistoryEntity>> = _historyPoints.asStateFlow()

    private val _inverterSummary = MutableStateFlow<InverterGenerationSummaryEntity?>(null)
    val inverterSummary: StateFlow<InverterGenerationSummaryEntity?> = _inverterSummary.asStateFlow()

    init {
        loadTelemetry()
    }

    private fun loadTelemetry() {
        viewModelScope.launch(errorHandler) {
            _isLoading.value = true
            val profile = repository.getCustomerProfileDirect()
            profile?.let {
                val customerId = it.id
                // Load inverter summary stats
                repository.refreshInverterTelemetry(customerId)
                repository.getInverterSummary(customerId).collect { sum ->
                    _inverterSummary.value = sum
                }
            }
            fetchHistory()
        }
    }

    private fun fetchHistory() {
        viewModelScope.launch(errorHandler) {
            val profile = repository.getCustomerProfileDirect()
            profile?.let {
                val customerId = it.id
                val period = _selectedPeriod.value
                // Maps standard backend routes: realtime, daily (as weekly/monthly), yearly
                val fetchPeriod = if (period == "weekly" || period == "monthly") "daily" else period
                repository.refreshInverterHistory(customerId, fetchPeriod)
                
                repository.getInverterHistory(customerId, fetchPeriod).collect { list ->
                    val filteredList = when (period) {
                        "weekly" -> list.takeLast(7) // limit to 7 days for weekly graph
                        else -> list
                    }
                    _historyPoints.value = filteredList
                    _isLoading.value = false
                }
            }
        }
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        fetchHistory()
    }

    fun refreshTracker() {
        viewModelScope.launch(errorHandler) {
            _isRefreshing.value = true
            val profile = repository.getCustomerProfileDirect()
            profile?.let {
                val customerId = it.id
                repository.refreshInverterTelemetry(customerId)
                val fetchPeriod = if (_selectedPeriod.value == "weekly" || _selectedPeriod.value == "monthly") "daily" else _selectedPeriod.value
                repository.refreshInverterHistory(customerId, fetchPeriod)
            }
            _isRefreshing.value = false
        }
    }
}
```

#### **2. Replace `TrackerScreen.kt` content:**

Replace code in `app/src/main/java/com/example/coustomerapp/ui/screens/tracker/TrackerScreen.kt` with a high-fidelity drawing canvas for spline area and bar charts, supporting drag gestures and interactive tooltips:

```kotlin
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.coustomerapp.ui.screens.dashboard.GlassCard
import com.example.coustomerapp.ui.theme.*
import com.example.coustomerapp.ui.viewmodel.TrackerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrackerViewModel = hiltViewModel()
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val historyPoints by viewModel.historyPoints.collectAsState()
    val summary by viewModel.inverterSummary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

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
                        "Generation Telemetry",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Period Selection Tabs
                val periods = listOf(
                    "realtime" to "Real-time",
                    "weekly" to "Weekly",
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

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Tooltip Info Box
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
                            text = "Timeframe: ${point.label}  •  Yield: $valueStr",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Drag finger across graph to inspect values", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Telemetry Drawing Canvas Card
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
                                Text("No telemetry logs found for this phase", color = TextSecondary)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                            onDrag = { change, _ ->
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

                                val maxVal = historyPoints.maxOfOrNull {
                                    if (selectedPeriod == "realtime") it.powerValue ?: 1.0
                                    else it.generationValue ?: 1.0
                                }?.coerceAtLeast(1.0) ?: 1.0

                                val stepX = if (historyPoints.size > 1) graphWidth / (historyPoints.size - 1) else graphWidth

                                touchX?.let { tx ->
                                    val relativeX = tx - padding
                                    val index = (relativeX / stepX).roundToInt().coerceIn(0, historyPoints.size - 1)
                                    activeIndex = index
                                }

                                // Axis divider
                                drawLine(
                                    color = GlassBorder,
                                    start = Offset(padding, height - padding),
                                    end = Offset(width - padding, height - padding),
                                    strokeWidth = 2f
                                )

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

                                    // Gradient fill
                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(EcoAccent.copy(alpha = 0.35f), Color.Transparent)
                                        )
                                    )

                                    // Line stroke
                                    drawPath(
                                        path = path,
                                        color = EcoAccent,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                } else {
                                    // Rounded Bars for weekly/monthly/yearly
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

                                // Interactive Tooltip Indicator
                                activeIndex?.let { idx ->
                                    val ptX = padding + idx * stepX
                                    val yVal = (if (selectedPeriod == "realtime") historyPoints[idx].powerValue else historyPoints[idx].generationValue) ?: 0.0
                                    val ptY = height - padding - (yVal / maxVal).toFloat() * graphHeight

                                    drawLine(
                                        color = Color.White.copy(alpha = 0.6f),
                                        start = Offset(ptX, padding),
                                        end = Offset(ptX, height - padding),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    drawCircle(
                                        color = PrimarySolar,
                                        radius = 6.dp.toPx(),
                                        center = Offset(ptX, ptY)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Dashboard Grid
                Text(
                    "Real-time Analytics Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                            Text("Today's Output", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "${summary?.dailyGeneration ?: 0.0} kWh",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }

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
                
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
```

---

### **Execution Instructions for Android Studio AI:**
1. Update `dto/CustomerDtos.kt` with the Open-Meteo DTOs and translation method.
2. Add the Open-Meteo Retrofit service paths to `ApiService.kt`.
3. Add the weather fetching code inside `CustomerRepository.kt`.
4. Expose the weather flow parameters inside `DashboardViewModel.kt` and trigger them from the customer profile city.
5. In `DashboardScreen.kt`, remove the carbon card and replace it with `WeatherWidgetCard`, and update the visual values in `EnergyVisualizationCard` to reflect load capacity.
6. Replace `TrackerViewModel.kt` and `TrackerScreen.kt` fully with the interactive graph rendering system above.
