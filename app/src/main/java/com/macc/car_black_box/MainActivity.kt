// MainActivity.kt
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
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.macc.car_black_box.models.AuthenticationModels.SigninRequest
import com.macc.car_black_box.models.Response
import com.macc.car_black_box.models.AuthenticationModels.SigninResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.pow
import kotlin.math.sqrt



class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private val accelData = mutableListOf<Pair<Long, FloatArray>>()
    private val gpsData = mutableListOf<Pair<Long, Location>>()

    private val CRASH_THRESHOLD = 30f // Adjust this empirically


    private fun hasAllLocationPermissions(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val background = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else false

        val sensors = ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
        Log.d("permissions", "fine location: ${fine}, coarse location: ${coarse}, background location: ${background}, sensors: ${sensors}")
        return fine && coarse && background && sensors
    }

    private fun getRequiredPermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.INTERNET
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        return perms.toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (!hasAllLocationPermissions()) {
            Log.d("permissions", "requestingg.......")
            ActivityCompat.requestPermissions(this, getRequiredPermissions(),100)
        }

        val loginButton = findViewById<Button>(R.id.loginButton)
        val goToRegister = findViewById<Button>(R.id.registerRedirect)

        loginButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()

            if (email.isNotBlank() && password.isNotBlank()) {
                remoteLogin(email, password)
                //if (email == "test@example.com" && password == "1234") {
                //    startActivity(Intent(this, TrackingActivity::class.java))
                //} else {
                //Toast.makeText(this, "Credenziali errate", Toast.LENGTH_SHORT).show()
                //}
            } else {
                Toast.makeText(this, "Completa tutti i campi", Toast.LENGTH_SHORT).show()
            }
        }

        goToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }


    fun remoteLogin(email: String, password: String){
        val retrofit = Retrofit.Builder()
            .baseUrl(CrashApiConstants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(CrashApiService::class.java)
        val call = apiService.signin(SigninRequest(email, password))
        call.enqueue(object : Callback<Response<SigninResponse>> {
            override fun onFailure(call: Call<Response<SigninResponse>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to connect to the remote server", Toast.LENGTH_LONG).show()
                t.printStackTrace()
            }

            override fun onResponse(call: Call<Response<SigninResponse>>, response: retrofit2.Response<Response<SigninResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val resp_obj = response.body()!!
                    if (resp_obj.status == "ok") {
                        val jwt = resp_obj.data?.token ?: "null-jwt"
                        val intent = Intent(this@MainActivity, TrackingActivity::class.java)
                        intent.putExtra("JWT_TOKEN", jwt)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, resp_obj.message ?: "generic error", Toast.LENGTH_LONG).show()
                    }
                } else if(response.code() == 404) { //non so perchè ma il body risulta vuoto
                    val message = response.body()?.message
                    Toast.makeText(this@MainActivity,  message ?: "wrong credentials", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Status code ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }
        })

    }
}
