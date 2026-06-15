package com.example.coustomerapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.coustomerapp.ui.navigation.Screen
import com.example.coustomerapp.ui.navigation.bottomNavItems
import com.example.coustomerapp.ui.screens.analytics.AnalyticsScreen
import com.example.coustomerapp.ui.screens.dashboard.DashboardScreen
import com.example.coustomerapp.ui.screens.dispatches.MaterialDispatchesScreen
import com.example.coustomerapp.ui.screens.login.LoginScreen
import com.example.coustomerapp.ui.screens.payments.PaymentsScreen
import com.example.coustomerapp.ui.screens.settings.SettingsScreen
import com.example.coustomerapp.ui.screens.service.ServiceRequestsScreen
import com.example.coustomerapp.ui.screens.tracker.TrackerScreen
import com.example.coustomerapp.ui.screens.tracker.InstallationJourneyScreen
import com.example.coustomerapp.ui.theme.*
import com.example.coustomerapp.ui.viewmodel.MainViewModel
import com.example.coustomerapp.ui.viewmodel.PaymentViewModel
import com.example.coustomerapp.util.SessionManager
import com.example.coustomerapp.util.biometric.BiometricHelper
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity(), PaymentResultWithDataListener {
    private val viewModel: MainViewModel by viewModels()
    private val paymentViewModel: PaymentViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started")
        enableEdgeToEdge()
        setContent {
            Log.d("MainActivity", "setContent started")
            CoustomerAppTheme {
                val fontSize by viewModel.fontSize.collectAsState()
                val density = LocalDensity.current
                val scale = when (fontSize) {
                    "Small" -> 0.85f
                    "Normal" -> 1.0f
                    "Large" -> 1.25f
                    else -> 1.0f
                }
                val customDensity = Density(density.density, density.fontScale * scale)

                CompositionLocalProvider(LocalDensity provides customDensity) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    val startDestination = try {
                        val loggedIn = viewModel.isLoggedIn()
                        Log.d("MainActivity", "isLoggedIn: $loggedIn")
                        if (loggedIn) Screen.Dashboard.route else Screen.Login.route
                    } catch (e: Throwable) {
                        Log.e("MainActivity", "Error checking logged in status", e)
                        Screen.Login.route
                    }
                    Log.d("MainActivity", "startDestination: $startDestination")

                    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

                    // Biometric Unlock Overlay Setup
                    val context = LocalContext.current
                    val prefs = remember { context.getSharedPreferences("swayog_settings", Context.MODE_PRIVATE) }
                    val biometricEnabled = prefs.getBoolean("biometric_enabled", false)
                    val timeoutEnabled = prefs.getBoolean("session_timeout_enabled", true)
                    val lastActive = prefs.getLong("last_active_timestamp", 0L)
                    val now = System.currentTimeMillis()

                    val isLoggedIn = viewModel.isLoggedIn()
                    val hasCredentials = sessionManager.getSavedLoginId() != null && sessionManager.getSavedPass() != null
                    val timeElapsed = now - lastActive
                    val shouldLock = isLoggedIn && biometricEnabled && hasCredentials && (lastActive == 0L || (timeoutEnabled && timeElapsed > 15 * 60 * 1000))

                    var isLocked by remember { mutableStateOf(shouldLock) }
                    val biometricHelper = remember { BiometricHelper(context) }

                    LaunchedEffect(isLocked) {
                        if (isLocked && biometricHelper.canAuthenticate()) {
                            biometricHelper.showBiometricPrompt(
                                activity = this@MainActivity,
                                onSuccess = {
                                    isLocked = false
                                    prefs.edit().putLong("last_active_timestamp", System.currentTimeMillis()).apply()
                                },
                                onError = { _, _ -> },
                                onFailed = {}
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent,
                            bottomBar = {
                                if (showBottomBar && !isLocked) {
                                    NavigationBar(
                                        containerColor = SurfaceDark.copy(alpha = 0.95f),
                                        contentColor = Color.White,
                                        tonalElevation = 0.dp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                    ) {
                                        bottomNavItems.forEach { screen ->
                                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                            NavigationBarItem(
                                                icon = {
                                                    screen.icon?.let { icon ->
                                                        Icon(
                                                            icon,
                                                            contentDescription = screen.title,
                                                            modifier = Modifier.size(if (selected) 26.dp else 22.dp)
                                                        )
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        screen.title,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                selected = selected,
                                                onClick = {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = PrimarySolar,
                                                    selectedTextColor = PrimarySolar,
                                                    indicatorColor = PrimarySolar.copy(alpha = 0.12f),
                                                    unselectedIconColor = TextTertiary,
                                                    unselectedTextColor = TextTertiary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                BackgroundDark,
                                                Color(0xFF0D1220),
                                                BackgroundDark
                                            )
                                        )
                                    )
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = startDestination,
                                    modifier = Modifier.padding(innerPadding),
                                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                                ) {
                                    composable(Screen.Login.route) {
                                        LoginScreen(
                                            onLoginSuccess = {
                                                prefs.edit().putLong("last_active_timestamp", System.currentTimeMillis()).apply()
                                                navController.navigate(Screen.Dashboard.route) {
                                                    popUpTo(Screen.Login.route) { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                    composable(Screen.Dashboard.route) {
                                        DashboardScreen(
                                            onNavigateToTracker = {
                                                navController.navigate(Screen.InstallationJourney.route)
                                            },
                                            onNavigateToDispatches = {
                                                navController.navigate(Screen.MaterialDispatches.route)
                                            },
                                            onNavigateToAnalytics = {
                                                navController.navigate(Screen.InstallationTracker.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            onLogout = {
                                                viewModel.logout()
                                                navController.navigate(Screen.Login.route) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                    composable(Screen.InstallationTracker.route) {
                                        TrackerScreen(
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }
                                    composable(Screen.InstallationJourney.route) {
                                        InstallationJourneyScreen(
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }
                                    composable(Screen.MaterialDispatches.route) {
                                        MaterialDispatchesScreen(
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }
                                    composable(Screen.ServiceRequests.route) {
                                        ServiceRequestsScreen(
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }
                                    composable(Screen.Payments.route) {
                                        PaymentsScreen(
                                            onNavigateBack = { navController.popBackStack() },
                                            viewModel = paymentViewModel
                                        )
                                    }
                                    composable(Screen.Analytics.route) {
                                        AnalyticsScreen(
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }
                                    composable(Screen.Settings.route) {
                                        SettingsScreen(
                                            onNavigateBack = { navController.popBackStack() },
                                            onLogoutRedirect = {
                                                navController.navigate(Screen.Login.route) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // App Lock Screen Overlay
                        if (isLocked) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(BackgroundDark)
                                    .clickable(enabled = false) {},
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    // Solar Logo
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(PrimarySolar, GradientEnd)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.WbSunny,
                                            contentDescription = "Logo",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text(
                                        "SWAYOG Secured",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color.White,
                                        letterSpacing = 2.sp
                                    )

                                    Text(
                                        "Unlock with fingerprint credentials to access dashboard metrics",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(32.dp))

                                    Button(
                                        onClick = {
                                            if (biometricHelper.canAuthenticate()) {
                                                biometricHelper.showBiometricPrompt(
                                                    activity = this@MainActivity,
                                                    onSuccess = {
                                                        isLocked = false
                                                        prefs.edit().putLong("last_active_timestamp", System.currentTimeMillis()).apply()
                                                    },
                                                    onError = { _, _ -> },
                                                    onFailed = {}
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimarySolar),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                    ) {
                                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Authenticate", fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    TextButton(
                                        onClick = {
                                            viewModel.logout()
                                            isLocked = false
                                            navController.navigate(Screen.Login.route) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    ) {
                                        Text("Sign Out / Use Password", color = PrimarySolar, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val prefs = getSharedPreferences("swayog_settings", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_active_timestamp", System.currentTimeMillis()).apply()
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        paymentData?.let {
            paymentViewModel.verifyPayment(
                orderId = it.orderId ?: "",
                paymentId = it.paymentId ?: "",
                signature = it.signature ?: ""
            )
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        paymentViewModel.handlePaymentFailure(response ?: "Payment cancelled or failed")
    }
}
