package com.example.coustomerapp.ui.screens.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.coustomerapp.data.local.entities.AmcVisit
import com.example.coustomerapp.data.local.entities.ServiceRequestEntity
import com.example.coustomerapp.ui.components.ShimmerCard
import com.example.coustomerapp.ui.theme.*
import com.example.coustomerapp.ui.viewmodel.ServiceViewModel
import com.example.coustomerapp.util.image.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRequestsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ServiceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val serviceRequests by viewModel.serviceRequests.collectAsState()
    val amcVisits by viewModel.amcVisits.collectAsState()
    val profile by viewModel.customerProfile.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showSheet by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tickets, 1: AMC Visits
    var selectedImages by remember { mutableStateOf<List<File>>(emptyList()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val files = uris.mapNotNull { uri ->
            ImageUtils.compressImage(context, uri)
        }
        selectedImages = selectedImages + files
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Service & Coordinator",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                containerColor = PrimarySolar,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Request")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshServiceData() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dual Tab Header component
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceDark,
                    contentColor = PrimarySolar,
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = PrimarySolar
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Service Tickets", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("AMC Visits", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        // Tickets list
                        if (isLoading && serviceRequests.isEmpty()) {
                            repeat(3) {
                                ShimmerCard(height = 140.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        } else if (serviceRequests.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(GlassWhite)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No active service tickets", color = TextSecondary)
                                    Text("Tap + to submit a ticket", color = TextTertiary, fontSize = 12.sp)
                                }
                            }
                        } else {
                            serviceRequests.forEach { request ->
                                ServiceRequestItem(request)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    } else {
                        // AMC Schedule list
                        if (amcVisits.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(GlassWhite)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No upcoming AMC cleanings", color = TextSecondary)
                                }
                            }
                        } else {
                            amcVisits.forEach { visit ->
                                AmcVisitItem(visit)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showSheet = false
                    selectedImages = emptyList()
                },
                containerColor = SurfaceDark
            ) {
                CreateServiceRequestContent(
                    selectedImages = selectedImages,
                    onPickImage = { imagePickerLauncher.launch("image/*") },
                    onDeleteImage = { selectedImages = selectedImages - it },
                    onSubmit = { type, desc, contact, lat, lon, addr ->
                        viewModel.submitRequest(
                            type = type,
                            description = "$desc (Contact: $contact)",
                            address = addr,
                            lat = lat,
                            lon = lon,
                            imageFiles = selectedImages
                        )
                        showSheet = false
                        selectedImages = emptyList()
                        Toast.makeText(context, "Request logged offline. Syncing shortly.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

@Composable
fun AmcVisitItem(visit: AmcVisit) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(visit.scheduledDate))
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PrimarySolar.copy(alpha = 0.15f),
                                GradientEnd.copy(alpha = 0.08f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = visit.visitNumber.toString(),
                    color = PrimarySolar,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Visit #${visit.visitNumber} (${visit.timeSlot ?: "Day Slot"})",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (visit.status == "completed") "Finished: $dateStr" else "Scheduled: $dateStr",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Technician: ${visit.technicianName}",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
            
            StatusBadge(visit.status)
        }
    }
}

@Composable
fun ServiceRequestItem(request: ServiceRequestEntity) {
    val dateStr = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = parser.parse(request.createdAt)
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date ?: Date())
    } catch (e: Exception) {
        request.createdAt.take(10)
    }

    val statusColor = when (request.status.lowercase()) {
        "pending" -> StatusPending
        "assigned" -> StatusAssigned
        "completed" -> StatusCompleted
        else -> TextTertiary
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .border(1.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                StatusBadge(request.status)
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = request.description,
                fontSize = 14.sp,
                color = TextSecondary,
                maxLines = 2
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = GlassBorder
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Logged on $dateStr",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!request.isSynced) {
                    Icon(
                        Icons.Default.CloudQueue,
                        contentDescription = "Pending Sync",
                        tint = WarningAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Offline queue", color = WarningAccent, fontSize = 11.sp)
                } else {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = "Synced",
                        tint = EcoAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status.lowercase()) {
        "pending" -> StatusPending
        "assigned" -> StatusAssigned
        "completed" -> StatusCompleted
        else -> TextTertiary
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CreateServiceRequestContent(
    selectedImages: List<File>,
    onPickImage: () -> Unit,
    onDeleteImage: (File) -> Unit,
    onSubmit: (String, String, String, Double, Double, String) -> Unit
) {
    var selectedType by remember { mutableStateOf("Panel Cleaning") }
    var description by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    // Coordinates default to Mumbai center
    var latitude by remember { mutableDoubleStateOf(19.123456) }
    var longitude by remember { mutableDoubleStateOf(72.890123) }
    var addressLabel by remember { mutableStateOf("Flat 402, Sunshine Residency, Andheri West, Mumbai") }

    val context = LocalContext.current
    val categories = listOf("Panel Cleaning", "Inverter Connection issue", "Structural check", "Electrical wiring")

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentGPSLocation(context) { loc ->
                latitude = loc.latitude
                longitude = loc.longitude
                addressLabel = "My GPS Site, Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}"
            }
        } else {
            Toast.makeText(context, "Location permission is required for site coordinates auto-tagging", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "New Service Request",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            "Describe the issue and pinpoint site coordinates",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Category", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(selectedType)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .background(SurfaceElevated)
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category, color = Color.White) },
                        onClick = {
                            selectedType = category
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Text("Contact Phone", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Technician calling number...", color = TextTertiary) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimarySolar,
                unfocusedBorderColor = GlassBorder,
                cursorColor = PrimarySolar
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Description", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            placeholder = { Text("Describe the issues in details...", color = TextTertiary) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimarySolar,
                unfocusedBorderColor = GlassBorder,
                cursorColor = PrimarySolar
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // GPS Coordinates manual/auto handlers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Installation Coordinates", color = TextSecondary, fontSize = 13.sp)
            TextButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        getCurrentGPSLocation(context) { loc ->
                            latitude = loc.latitude
                            longitude = loc.longitude
                            addressLabel = "My GPS Site, Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}"
                        }
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = PrimarySolar, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Get GPS", color = PrimarySolar, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Interactive Canvas Map Grid with Draggable Pin
        var dragOffset by remember { mutableStateOf(Offset(200f, 150f)) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            dragOffset = offset
                            // Scale coordinates around center
                            latitude = 19.123456 + (offset.y - 150f) / 100000.0
                            longitude = 72.890123 + (offset.x - 200f) / 100000.0
                            addressLabel = "Site pin placed at ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw dark blueprint background grid lines
                drawRect(color = BackgroundDark)
                for (x in 0..w.toInt() step 40) {
                    drawLine(color = GlassBorder.copy(alpha = 0.3f), start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), h))
                }
                for (y in 0..h.toInt() step 40) {
                    drawLine(color = GlassBorder.copy(alpha = 0.3f), start = Offset(0f, y.toFloat()), end = Offset(w, y.toFloat()))
                }

                // Draw target site center marker
                drawCircle(
                    color = EcoAccent.copy(alpha = 0.2f),
                    radius = 30f,
                    center = dragOffset
                )

                drawCircle(
                    color = EcoAccent,
                    radius = 8f,
                    center = dragOffset
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(SurfaceDark.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Tap map to position pin", color = TextSecondary, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "📍 $addressLabel",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Photo Proofs", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Attach Photos (Compress to < 500KB)")
        }

        if (selectedImages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedImages) { file ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        val bitmap = remember(file) {
                            try {
                                BitmapFactory.decodeFile(file.absolutePath)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(GlassWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = TextSecondary)
                            }
                        }
                        IconButton(
                            onClick = { onDeleteImage(file) },
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { onSubmit(selectedType, description, contactPhone, latitude, longitude, addressLabel) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(14.dp),
            enabled = description.isNotBlank() && contactPhone.isNotBlank(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (description.isNotBlank() && contactPhone.isNotBlank())
                                listOf(PrimarySolar, GradientEnd)
                            else
                                listOf(PrimarySolar.copy(alpha = 0.3f), GradientEnd.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Submit Service Request", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Native GPS retrieval function using LocationManager
private fun getCurrentGPSLocation(context: Context, onResult: (Location) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val provider = when {
            hasGps -> LocationManager.GPS_PROVIDER
            hasNetwork -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider != null) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                onResult(location)
            } else {
                // Return fallback Mumbai location
                val fallback = Location("fallback").apply {
                    latitude = 19.123456
                    longitude = 72.890123
                }
                onResult(fallback)
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}
