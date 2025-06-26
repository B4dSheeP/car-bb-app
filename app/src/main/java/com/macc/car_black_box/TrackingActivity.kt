// TrackingActivity.kt
package com.macc.car_black_box

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.location.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt
import android.util.Log
import androidx.activity.viewModels
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


import com.macc.car_black_box.AuthViewModel
import com.macc.car_black_box.models.Response
import com.macc.car_black_box.models.CrashModels
import com.macc.car_black_box.CrashApiService
import com.macc.car_black_box.CrashApiConstants
import com.macc.car_black_box.CrashActivity

class TrackingActivity : AppCompatActivity() {
    private lateinit var authViewModel: AuthViewModel
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val accelData = mutableListOf<Pair<Long, FloatArray>>()
    private val gpsData = mutableListOf<Pair<Long, Location>>()
    private var last_accel_instant: Long = 0
    private var last_crash_instant: Long = 0
    private val CRASH_THRESHOLD = 49f //16f //m/s*s 5g per un crash... non penso nessuna auto accelleri a 5g

    private var status_tracking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)
        status_tracking = false
        val startBtn = findViewById<Button>(R.id.startTrackingButton)
        val stopBtn = findViewById<Button>(R.id.stopTrackingButton)
        val viewHistoryBtn = findViewById<Button>(R.id.viewHistoryButton)
        val statusText = findViewById<TextView>(R.id.trackingStatus)


        val tokenFromIntent = intent.getStringExtra("JWT_TOKEN")
        authViewModel = AuthViewModel(tokenFromIntent ?: "null-jwt")
        Log.d("LOGGED IN ", authViewModel.jwtToken ?: "not-token")


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    //val now = System.currentTimeMillis()
                    val now = location.time
                    synchronized(gpsData) {
                        gpsData.add(Pair(now, location))
                        if(gpsData.size > 500) gpsData.removeAll { it.first < now - 40000 }
                    }
                    Log.d("TrackingActivity", "Gps data: $location $now")
                }
            }
        }

        startBtn.setOnClickListener {
            status_tracking = true
            initTracking()
            statusText.text = "Tracking: ON"
            Log.d(TrackingActivity::class.java.name, "Tracking enabled")
        }

        stopBtn.setOnClickListener {
            sensorManager.unregisterListener(sensorListener)
            //locationManager.removeUpdates(locationListener)
            fusedLocationClient.removeLocationUpdates(locationCallback)
            statusText.text = "Tracking: OFF"
            status_tracking = false
            Log.d(TrackingActivity::class.java.name, "Tracking disabled")
        }
        viewHistoryBtn.setOnClickListener {
            val jwt = tokenFromIntent ?: "null-jwt"
            val intent = Intent(this@TrackingActivity, CrashActivity::class.java)
            intent.putExtra("JWT_TOKEN", jwt)
            startActivity(intent)
        }

    }

    private fun initTracking() {

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),100)
        }
        Log.d("GPS", "gps granted")
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(500)
            .build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100L, 0f, locationListener)
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val now = System.currentTimeMillis()
            if (now - last_accel_instant < 100) return //registro val acc ogni 100 millisecondi
            last_accel_instant = now
            synchronized(accelData) {
                accelData.add(Pair(now, event.values.clone()))
                //Log.d(TrackingActivity::class.java.name, "Accel data: ${ss}")
                if(accelData.size > 500) accelData.removeAll { it.first < now - 40000 }
            }

            val accNoGravity = sqrt(event.values[0].pow(2) + event.values[1].pow(2) + (event.values[2] - 9.81 ).pow(2))

            if (accNoGravity > CRASH_THRESHOLD && now - last_crash_instant > 200) { //200 millisecondi ogni crash
                last_crash_instant = now
                onCrashDetected(now)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
/*
    private val locationListener = LocationListener { location ->
        val now = System.currentTimeMillis()
        Log.d(TrackingActivity::class.java.name, "outside Gps data: ${gpsData.takeLast(1)}")
        synchronized(gpsData) {
            gpsData.add(Pair(now, location))
            Log.d(TrackingActivity::class.java.name, "Gps data: ${gpsData.takeLast(1)}")
            gpsData.removeAll { it.first < now - 20000 } //questo lo farei tirando una moneta, non serve farlo sempre.
        }
    }
*/

    private fun onCrashDetected(crashTime: Long) {
        val before = synchronized(accelData){ accelData.filter { it.first in (crashTime - 20000)..crashTime } }
        val after = mutableListOf<Pair<Long, FloatArray>>()
        val gpsSnapshot = synchronized(gpsData){gpsData.filter{ it.first in crashTime - 20000..crashTime}.toList()}
        val gpsSnapshotafter = mutableListOf<Pair<Long, Location>>()
        val logText = findViewById<TextView>(R.id.logText)
        logText.text = "\n Crash detected at ${crashTime}"

        lifecycleScope.launch(Dispatchers.Default) {
            val end = crashTime + 20000
            while (System.currentTimeMillis() < end) {
                delay(200)
            }
            synchronized(accelData){ after.addAll(accelData.filter { it.first in crashTime..end }) }
            synchronized(gpsData) { gpsSnapshotafter.addAll(gpsData.filter{ it.first in crashTime..end })}
            sendDataToServer(crashTime, before, after, gpsSnapshot.plus(gpsSnapshotafter))
        }
    }

    private fun sendDataToServer(
        crashTime: Long,
        before: List<Pair<Long, FloatArray>>,
        after: List<Pair<Long, FloatArray>>,
        gps: List<Pair<Long, Location>>
    ) {
        Log.d("CRASH", "${crashTime} ${before.size} ${after.size} ${gps.size}")
        // Here you would typically create a Retrofit instance and make the API call
        val retrofit = Retrofit.Builder().
            baseUrl(CrashApiConstants.BASE_URL).
            client(authViewModel.getOkHttpClient()).
            addConverterFactory(GsonConverterFactory.create()).
            build()
        val crashApiService = retrofit.create(CrashApiService::class.java)
        val crashReport = CrashModels.CrashReport(
            timestamp = crashTime,
            accel_data = before.map { CrashModels.AccelData(it.first, it.second[0], it.second[1], it.second[2]) } +
                        after.map { CrashModels.AccelData(it.first, it.second[0], it.second[1], it.second[2]) }
            ,
            gps_data = gps.map { CrashModels.GpsData(it.first, it.second.latitude.toFloat(), it.second.longitude.toFloat(), it.second.altitude.toFloat(), it.second.speed) }
        )
        Log.d("CRASH", "Sending crash report: $crashReport")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = crashApiService.new_crash(crashReport).execute()
                if (response.isSuccessful) {
                    Log.d("CRASH", "Crash report sent successfully: ${response.body()}")
                    runOnUiThread {
                        Toast.makeText(this@TrackingActivity, "Crash data sent successfully", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.e("CRASH", "Failed to send crash report: ${response.errorBody()?.string()}")
                    runOnUiThread {
                        Toast.makeText(this@TrackingActivity, "Failed to send crash data", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CRASH", "Error sending crash report: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@TrackingActivity, "Error sending crash data", Toast.LENGTH_LONG).show()
                }
            }
        }
        //
        //runOnUiThread {
        //    1 + 1
        ////    Toast.makeText(this, "Crash data ready to send", Toast.LENGTH_LONG).show()
        //}
    }

    public override fun onResume() {
        super.onResume()
        if(status_tracking == true){
            val statusText = findViewById<TextView>(R.id.trackingStatus)
            initTracking()
            statusText.text = "Tracking: ON"
            Log.d(TrackingActivity::class.java.name, "Tracking enabled")
        }
    }

}

/* add a foreground service to survive when screen blocked" */