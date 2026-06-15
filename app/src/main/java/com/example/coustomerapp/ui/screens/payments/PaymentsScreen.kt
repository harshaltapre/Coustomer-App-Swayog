package com.example.coustomerapp.ui.screens.payments

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.coustomerapp.data.local.entities.InvoiceEntity
import com.example.coustomerapp.ui.theme.*
import com.example.coustomerapp.ui.viewmodel.PaymentEvent
import com.example.coustomerapp.ui.viewmodel.PaymentViewModel
import com.razorpay.Checkout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val balance by viewModel.outstandingBalance.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val paymentEvent by viewModel.paymentEvent.collectAsState()
    val context = LocalContext.current as Activity
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(paymentEvent) {
        when (val event = paymentEvent) {
            is PaymentEvent.StartRazorpay -> {
                val checkout = Checkout()
                checkout.setKeyID(event.key)
                try {
                    val options = JSONObject()
                    options.put("name", "SWAYOG Solar Portal")
                    options.put("description", "Outstanding Dues Payment")
                    options.put("order_id", event.orderId)
                    options.put("theme.color", "#FF6B00")
                    options.put("currency", "INR")
                    options.put("amount", (event.amount * 100).toInt())
                    
                    val prefill = JSONObject()
                    val profile = viewModel.customerProfile.value
                    prefill.put("name", profile?.fullName ?: "John Doe")
                    prefill.put("email", profile?.email ?: "john@example.com")
                    options.put("prefill", prefill)

                    checkout.open(context, options)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error starting payment: ${e.message}")
                }
                viewModel.clearEvent()
            }
            is PaymentEvent.PaymentSuccess -> {
                snackbarHostState.showSnackbar("Payment successful! Outstanding invoices updated.")
                viewModel.clearEvent()
            }
            is PaymentEvent.PaymentFailed -> {
                snackbarHostState.showSnackbar("Payment failed: ${event.message}")
                viewModel.clearEvent()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Payments & Invoices",
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Outstanding Balance Dues Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PrimarySolar.copy(alpha = 0.2f),
                                GradientEnd.copy(alpha = 0.08f),
                                GlassWhite
                            )
                        )
                    )
                    .border(1.dp, PrimarySolar.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimarySolar.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = PrimarySolar,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Outstanding Dues", color = TextSecondary, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "₹${String.format("%,.2f", balance)}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { viewModel.initiateRazorpayPayment(balance) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(14.dp),
                        enabled = balance > 0,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = if (balance > 0)
                                            listOf(PrimarySolar, GradientEnd)
                                        else
                                            listOf(
                                                PrimarySolar.copy(alpha = 0.3f),
                                                GradientEnd.copy(alpha = 0.3f)
                                            )
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Pay Outstanding Dues", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Text(
                "Invoice Ledger",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (invoices.isEmpty()) {
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
                            Icons.Default.History,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No invoice records found", color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(invoices) { invoice ->
                        InvoiceLedgerItem(invoice, onPayClick = {
                            viewModel.initiateRazorpayPayment(invoice.amount)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceLedgerItem(invoice: InvoiceEntity, onPayClick: () -> Unit) {
    val context = LocalContext.current
    val isPaid = invoice.paymentStatus.lowercase() == "paid"
    
    val dateStr = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = parser.parse(invoice.invoiceDate)
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date ?: Date())
    } catch (e: Exception) {
        invoice.invoiceDate.take(10)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Abbreviated Monospace ID, Tap to Copy
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Invoice ID", invoice.id)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Invoice ID copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = invoice.id.take(8).uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy ID",
                        tint = TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Invoice Type Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(InfoAccent.copy(alpha = 0.12f))
                        .border(1.dp, InfoAccent.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = invoice.invoiceType.uppercase(),
                        color = InfoAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = invoice.description ?: "${invoice.invoiceType.replaceFirstChar { it.uppercase() }} invoice",
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Invoice Date", color = TextTertiary, fontSize = 11.sp)
                    Text(dateStr, fontSize = 12.sp, color = TextSecondary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Amount", color = TextTertiary, fontSize = 11.sp)
                    Text(
                        text = "₹${String.format("%,.2f", invoice.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = GlassBorder
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Payment Status Badge
                    val statusColor = if (isPaid) EcoAccent else WarningAccent
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = invoice.paymentStatus.uppercase(),
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Receipt Attachment View Action
                    if (!invoice.proofUrl.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = "View Receipt",
                            tint = InfoAccent,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    Toast.makeText(context, "Opening receipt: ${invoice.proofUrl}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        )
                    }
                }

                // If pending, show pay button action
                if (!isPaid) {
                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimarySolar),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Pay Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
