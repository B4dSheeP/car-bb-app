package com.macc.car_black_box

import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.macc.car_black_box.models.CrashModels

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenFromIntent = intent.getStringExtra("JWT_TOKEN")
        val crashViewModel = CrashViewModel(tokenFromIntent ?: "null-jwt")
        setContent {
            CrashScreen(crashViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashScreen(crashViewModel: CrashViewModel) {
    val crashes by remember { mutableStateOf(crashViewModel.crashes) }

    LaunchedEffect(Unit) {
        crashViewModel.fetchCrashes()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Crash Summary") })
        }
    ) { padding -> Box(modifier = Modifier.padding(padding)) {
        Log.d("CRASHES", crashes.value.toString())
        CrashSummary(crashes.value)
    }
    }
}

@Composable
fun CrashSummary(crashes: List<CrashModels.CrashReport>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(crashes.size) { index ->
            CrashCard(crashes[index])
        }
    }
}

@Composable
fun CrashCard(crash: CrashModels.CrashReport) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Crash Summary", style = MaterialTheme.typography.titleMedium)
            Text(text = crash.timestamp.toString(), style = MaterialTheme.typography.bodyMedium) // Replace `summary` with the actual field
        }
    }
}