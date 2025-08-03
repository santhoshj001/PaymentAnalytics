package com.sjdroid.paymentanalytics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.sjdroid.paymentanalytics.AnalyticsData
import com.sjdroid.paymentanalytics.domain.repository.ConnectionState
import com.sjdroid.paymentanalytics.presentation.model.AnalyticsUiState
import com.sjdroid.paymentanalytics.presentation.viewmodel.AnalyticsViewModel
import com.sjdroid.paymentanalytics.presentation.viewmodel.ViewModelFactory
import com.sjdroid.paymentanalytics.ui.theme.PaymentAnalyticsTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    
    private lateinit var analyticsViewModel: AnalyticsViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        analyticsViewModel = ViewModelProvider(
            this,
            ViewModelFactory(this)
        )[AnalyticsViewModel::class.java]
        
        setContent {
            PaymentAnalyticsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PaymentAnalyticsScreen(
                        viewModel = analyticsViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentAnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var transactionMessage by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Text(
            text = "Payment Analytics Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        ConnectionStatusCard(uiState.connectionState)
        
        if (uiState.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(8.dp))
                Text("Loading analytics data...")
            }
        }
        
        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        uiState.analyticsData?.let { data ->
            AnalyticsDataCard(data)
        }
        
        DeviceReadinessCard(uiState.isDeviceReady)
        
        PaymentActionsCard(
            canPerformTransaction = uiState.canPerformTransaction,
            onTransactionStart = { 
                transactionMessage = viewModel.simulateTransactionStart()
            },
            onPostTransaction = { viewModel.simulatePostTransaction() },
            onMaintenanceDiagnostics = { viewModel.performMaintenanceDiagnostics() },
            onRefresh = { viewModel.refreshAnalyticsData() }
        )
        
        if (transactionMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (transactionMessage.contains("successfully")) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = transactionMessage,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(connectionState: ConnectionState) {
    val (statusText, statusColor) = when (connectionState) {
        is ConnectionState.Disconnected -> "Disconnected" to Color.Red
        is ConnectionState.Connecting -> "Connecting..." to Color(0xFFFF9800)
        is ConnectionState.Connected -> "Connected" to Color.Green
        is ConnectionState.Error -> "Error: ${connectionState.message}" to Color.Red
    }
    
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Service Status: ",
                fontWeight = FontWeight.Medium
            )
            Text(
                text = statusText,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AnalyticsDataCard(data: AnalyticsData) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Device Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            MetricRow("Battery Level", "${data.batteryLevel}%")
            MetricRow("Memory Usage", String.format("%.1f%%", data.memoryUsagePercent))
            MetricRow("CPU Usage", String.format("%.1f%%", data.cpuUsagePercent))
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Transaction Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            MetricRow("Total Transactions", data.transactionCount.toString())
            MetricRow("Successful", data.successfulTransactions.toString())
            MetricRow("Failed", data.failedTransactions.toString())
            
            if (data.lastTransactionTime > 0) {
                val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                MetricRow("Last Transaction", formatter.format(Date(data.lastTransactionTime)))
            }
            
            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            MetricRow("Last Updated", formatter.format(Date(data.timestamp)))
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:")
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DeviceReadinessCard(isReady: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isReady) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Device Status: ",
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isReady) "Ready for Transactions" else "Not Ready",
                fontWeight = FontWeight.Bold,
                color = if (isReady) Color.Green else Color.Red
            )
        }
    }
}

@Composable
fun PaymentActionsCard(
    canPerformTransaction: Boolean,
    onTransactionStart: () -> Unit,
    onPostTransaction: () -> Unit,
    onMaintenanceDiagnostics: () -> Unit,
    onRefresh: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Payment Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onTransactionStart,
                modifier = Modifier.fillMaxWidth(),
                enabled = canPerformTransaction
            ) {
                Text("Start Transaction")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onPostTransaction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Post-Transaction Upload")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onMaintenanceDiagnostics,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Maintenance Diagnostics")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Analytics")
            }
        }
    }
}