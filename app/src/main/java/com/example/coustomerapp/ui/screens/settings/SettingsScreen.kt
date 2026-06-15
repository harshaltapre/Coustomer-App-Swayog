package com.example.coustomerapp.ui.screens.settings

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.coustomerapp.data.local.entities.SavedCardEntity
import com.example.coustomerapp.ui.screens.dashboard.GlassCard
import com.example.coustomerapp.ui.theme.*
import com.example.coustomerapp.ui.viewmodel.SettingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogoutRedirect: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val profile by viewModel.customerProfile.collectAsState()
    val savedCards by viewModel.savedCards.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val is2FAEnabled by viewModel.is2FAEnabled.collectAsState()
    val isSessionTimeoutEnabled by viewModel.isSessionTimeoutEnabled.collectAsState()
    val profileAvatarPath by viewModel.profileAvatarPath.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.saveAvatarImage(it) }
    }

    var cardHolderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var showAddCardDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Profile",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Profile & Avatar Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Image
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(GlassWhite)
                            .border(2.dp, PrimarySolar, CircleShape)
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarFile = profileAvatarPath?.let { File(it) }
                        if (avatarFile != null && avatarFile.exists()) {
                            val bitmap = remember(profileAvatarPath) {
                                BitmapFactory.decodeFile(avatarFile.absolutePath)
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Profile Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            }
                        } else {
                            val firstChar = profile?.fullName?.firstOrNull()?.toString() ?: "U"
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(PrimarySolar, GradientEnd)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = firstChar,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Camera overlay badge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimarySolar)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = "Change avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = profile?.fullName ?: "User Name",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = profile?.email ?: "email@example.com",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Installation stage & metadata summary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassWhite)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Customer Code", color = TextTertiary, fontSize = 10.sp)
                            Text(profile?.customerCode ?: "N/A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(GlassBorder)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("System Size", color = TextTertiary, fontSize = 10.sp)
                            Text("${profile?.systemSizeKw ?: 0.0} kW", color = EcoAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(GlassBorder)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AMC Status", color = TextTertiary, fontSize = 10.sp)
                            val isActive = profile?.amcStatus?.lowercase() == "active"
                            Text(
                                text = if (isActive) "Active" else "None",
                                color = if (isActive) EcoAccent else ErrorAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Settings Section
            Text(
                "App Preferences",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Font Size Preference
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Font Size Options", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Adjust system text scaling", color = TextSecondary, fontSize = 11.sp)
                        }

                        // Custom Chips Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Small", "Normal", "Large").forEach { size ->
                                val isSelected = fontSize == size
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimarySolar else GlassWhite)
                                        .border(1.dp, if (isSelected) PrimarySolar else GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setFontSize(size) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = size,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
                    Spacer(modifier = Modifier.height(20.dp))

                    // Security: 2FA Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Two-Factor Authentication", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Secure account transactions", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = is2FAEnabled,
                            onCheckedChange = { viewModel.set2FAEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimarySolar,
                                checkedTrackColor = PrimarySolar.copy(alpha = 0.4f),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = GlassWhite
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
                    Spacer(modifier = Modifier.height(20.dp))

                    // Security: Session Timeout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Session Timeout Lock", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Lock app after 15m inactivity", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isSessionTimeoutEnabled,
                            onCheckedChange = { viewModel.setSessionTimeoutEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimarySolar,
                                checkedTrackColor = PrimarySolar.copy(alpha = 0.4f),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = GlassWhite
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card Vault Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Local Card Vault",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                TextButton(
                    onClick = { showAddCardDialog = true },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PrimarySolar, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Card", color = PrimarySolar, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (savedCards.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No cards saved locally", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    savedCards.forEach { card ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Card Brand Icon Representation
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrimarySolar.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = PrimarySolar,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = card.maskedCardNumber,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(card.cardHolderName, color = TextSecondary, fontSize = 11.sp)
                                            Text("Exp: ${card.expiryDate}", color = TextSecondary, fontSize = 11.sp)
                                            Text(card.cardBrand, color = EcoAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.removeSavedCard(card) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete card",
                                        tint = ErrorAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Everywhere Button
            Button(
                onClick = { viewModel.signOutEverywhere(onLogoutRedirect) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorAccent.copy(alpha = 0.15f),
                    contentColor = ErrorAccent
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorAccent.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out From All Devices", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Add Card Dialog
    if (showAddCardDialog) {
        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = {
                Text(
                    "Add Saved Card",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Your card number will be hashed locally with SHA-256 for identification. CVV is never stored.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = cardHolderName,
                        onValueChange = { cardHolderName = it },
                        label = { Text("Cardholder Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimarySolar,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = PrimarySolar
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { input ->
                            // Simple format spacing mapping
                            val cleanInput = input.replace(" ", "")
                            if (cleanInput.length <= 16 && cleanInput.all { it.isDigit() }) {
                                cardNumber = cleanInput.chunked(4).joinToString(" ")
                            }
                        },
                        label = { Text("Card Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimarySolar,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = PrimarySolar
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { input ->
                            val cleanInput = input.replace("/", "")
                            if (cleanInput.length <= 4 && cleanInput.all { it.isDigit() }) {
                                expiryDate = if (cleanInput.length >= 2) {
                                    cleanInput.substring(0, 2) + "/" + cleanInput.substring(2)
                                } else {
                                    cleanInput
                                }
                            }
                        },
                        label = { Text("Expiry (MM/YY)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimarySolar,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = PrimarySolar
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (cardHolderName.isNotBlank() && cardNumber.length >= 15 && expiryDate.length == 5) {
                            viewModel.addSavedCard(cardHolderName, cardNumber, expiryDate)
                            // Reset fields
                            cardHolderName = ""
                            cardNumber = ""
                            expiryDate = ""
                            showAddCardDialog = false
                        }
                    }
                ) {
                    Text("Save Card", color = PrimarySolar, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = Color.White,
            textContentColor = TextSecondary
        )
    }
}
