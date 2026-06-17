package com.example.coustomerapp.ui.screens.service

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.coustomerapp.data.local.entities.ServiceRequestEntity
import com.example.coustomerapp.ui.theme.*
import com.example.coustomerapp.ui.viewmodel.ServiceRequestViewModel
import com.google.android.gms.location.LocationServices
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRequestsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ServiceRequestViewModel = hiltViewModel()
) {
    val context       = LocalContext.current
    val allRequests   by viewModel.allRequests.collectAsState()
    val isSubmitting  by viewModel.isSubmitting.collectAsState()
    val submitSuccess by viewModel.submitSuccess.collectAsState()

    var showSheet     by remember { mutableStateOf(false) }
    var serviceType   by remember { mutableStateOf("Panel Cleaning") }
    var description   by remember { mutableStateOf("") }
    var address       by remember { mutableStateOf("") }
    var latitude      by remember { mutableStateOf(0.0) }
    var longitude     by remember { mutableStateOf(0.0) }
    var imageUri      by remember { mutableStateOf<Uri?>(null) }
    var imageFile     by remember { mutableStateOf<File?>(null) }
    val preferredDate = LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)

    val serviceTypes = listOf("Panel Cleaning", "Inverter Issue", "Structural Check", "Electrical Wiring", "Other")

    // Camera / gallery launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val tempFile    = File(context.cacheDir, "ticket_image_${System.currentTimeMillis()}.jpg")
            tempFile.outputStream().use { out -> inputStream?.copyTo(out) }
            imageFile = tempFile
        }
    }

    // Location helper
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                locationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let { latitude = it.latitude; longitude = it.longitude }
                }
            }
        }
    }

    fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { latitude = it.latitude; longitude = it.longitude }
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(submitSuccess) {
        if (submitSuccess) { showSheet = false; viewModel.resetSubmitState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Requests", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showSheet = true; fetchLocation() }) {
                        Icon(Icons.Default.Add, null, tint = PrimarySolar)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier             = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement  = Arrangement.spacedBy(12.dp)
        ) {
            items(allRequests) { request -> ServiceRequestCard(request) }
        }

        // ── Bottom sheet: Create ticket ──
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest  = { showSheet = false },
                containerColor    = SurfaceDark,
                sheetState        = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("New service request", fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = Color.White)
                    Spacer(Modifier.height(16.dp))

                    // Service type dropdown
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value            = serviceType,
                            onValueChange    = {},
                            readOnly         = true,
                            label            = { Text("Service category") },
                            trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors           = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = PrimarySolar,
                                unfocusedBorderColor = GlassBorder,
                                focusedLabelColor     = PrimarySolar,
                                unfocusedLabelColor   = TextSecondary,
                                focusedTextColor      = Color.White,
                                unfocusedTextColor    = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            serviceTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, color = Color.White) },
                                    onClick = {
                                        serviceType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value          = description,
                        onValueChange  = { description = it },
                        label          = { Text("Description") },
                        modifier       = Modifier.fillMaxWidth().height(100.dp),
                        colors         = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimarySolar,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value          = address,
                        onValueChange  = { address = it },
                        label          = { Text("Address") },
                        modifier       = Modifier.fillMaxWidth(),
                        colors         = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimarySolar,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // GPS Coordinates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = { fetchLocation() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimarySolar)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Get GPS")
                            Spacer(Modifier.width(4.dp))
                            Text("GPS", color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Photo selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Photo Proof", color = Color.White)
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = InfoAccent)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                            Spacer(Modifier.width(4.dp))
                            Text("Attach", color = Color.White)
                        }
                    }

                    imageUri?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("Attached: $it", color = EcoAccent, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.submitRequest(
                                context       = context,
                                serviceType   = serviceType,
                                description   = description,
                                address       = address,
                                latitude      = latitude,
                                longitude     = longitude,
                                preferredDate = preferredDate,
                                imageFile     = imageFile
                            )
                        },
                        modifier      = Modifier.fillMaxWidth().height(48.dp),
                        colors        = ButtonDefaults.buttonColors(containerColor = PrimarySolar),
                        enabled       = description.isNotBlank() && address.isNotBlank() && !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Submit Request", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceRequestCard(request: ServiceRequestEntity) {
    val statusColor = when (request.status.lowercase()) {
        "pending" -> WarningAccent
        "completed" -> EcoAccent
        else -> InfoAccent
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(request.serviceType, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(request.status.uppercase(), color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(request.description, color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("Address: ${request.address}", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Logged: ${request.createdAt.take(10)}", color = TextSecondary, fontSize = 11.sp)
                if (request.isSynced) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = EcoAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Synced", color = EcoAccent, fontSize = 11.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudQueue, contentDescription = "Pending Sync", tint = WarningAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Offline Queue", color = WarningAccent, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
