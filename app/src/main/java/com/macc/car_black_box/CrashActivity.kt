package com.macc.car_black_box

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat

import com.macc.car_black_box.models.CrashModels
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenFromIntent = intent.getStringExtra("JWT_TOKEN")
        val crashViewModel = CrashViewModel(tokenFromIntent ?: "null-jwt")
        setContent{ CrashScreen(crashViewModel) }
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
    var expandedCrash by remember { mutableStateOf<CrashModels.CrashReport?>(null) }

    Column {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(crashes.size) { index ->
                val crash = crashes[index]
                CrashCard(
                    crash = crash,
                    isExpanded = expandedCrash?.timestamp == crash.timestamp,
                    onClick = {
                        expandedCrash = if (expandedCrash?.timestamp == crash.timestamp) null else crash
                    }
                )
            }
        }

        expandedCrash?.let{CrashDetail(it)}
    }
}

@Composable
fun CrashCard(crash: CrashModels.CrashReport, isExpanded: Boolean, onClick: () -> Unit) {
    Card(modifier= Modifier.width(200.dp).height(100.dp).clickable { onClick() },
        elevation= CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier= Modifier.padding(8.dp), verticalArrangement= Arrangement.Center) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            val datetime = sdf.format(crash.timestamp)
            Text(text = "Crash event", style = MaterialTheme.typography.titleMedium)
            Text(text = "Date time: $datetime", style = MaterialTheme.typography.bodyMedium)
        }
    }
}


@Composable
fun CrashDetail(crash: CrashModels.CrashReport) {
    val crashPath = remember { crash.gps_data.map { LatLng(it.latitude.toDouble(), it.longitude.toDouble()) } }
    val first_latlng =  crashPath.first()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(first_latlng, 15f)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.padding(8.dp).fillMaxWidth().verticalScroll(scrollState)
    ) {
        Text("Crash Route (${crash.gps_data.size} points)", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        GoogleMap(modifier = Modifier.fillMaxWidth().height(300.dp), cameraPositionState = cameraPositionState) {
            Polyline(points = crashPath, color = Color.Red, width = 5f)
            crash.gps_data.forEach { point ->
                val delta = ((point.instant - crash.timestamp)/100).toFloat()/10.0
                val kh_speed = (point.speed * 3.6).roundToInt()
                val label = "Δt: ${delta}s, Speed: ${kh_speed} k/h"
                Marker(
                    state = MarkerState(position = LatLng(point.latitude.toDouble(), point.longitude.toDouble())),
                    title = label
                )
            }
        }
        crash.accel_data.forEach{ acc_d ->
            val delta = ((acc_d.instant - crash.timestamp)/100).toFloat()/10.0
            val acc = (sqrt(acc_d.x.pow(2)+acc_d.y.pow(2)+acc_d.z.pow(2))*100).roundToInt()/100f
            val x = (acc_d.x*100).roundToInt()/100f
            val y = (acc_d.y*100).roundToInt()/100f
            val z = (acc_d.z*100).roundToInt()/100f
            val t = "Δt: ${delta}s, Total acc.: ${acc} m/s^2 -> x ${x} - y ${y} - z ${z}"
            Text(t, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
