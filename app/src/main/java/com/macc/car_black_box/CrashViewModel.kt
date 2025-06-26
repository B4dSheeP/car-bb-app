package com.macc.car_black_box


import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.awaitResponse


import com.macc.car_black_box.models.CrashModels


class CrashViewModel(jwtToken: String) : ViewModel() {
    private val jwtToken: String = jwtToken
    private val _crashes = mutableStateOf<List<CrashModels.CrashReport>>(emptyList())
    val crashes: State<List<CrashModels.CrashReport>> = _crashes

    private val apiService: CrashApiService by lazy {
        Retrofit.Builder()
            .baseUrl(CrashApiConstants.BASE_URL) // Replace with your API base URL
            .client(AuthViewModel(jwtToken).getOkHttpClient()) // Use the OkHttpClient with JWT token
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CrashApiService::class.java)
    }

    fun fetchCrashes() {
        Log.d("CRASHVIEWMODEL", "fetching crashes")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.get_crashes().awaitResponse()
                if (response.isSuccessful) {
                    val crashReports = response.body()?.data ?: emptyList()
                    withContext(Dispatchers.Main) {
                        _crashes.value = crashReports
                    }
                } else {
                    Log.d("CRASHVIEWMODEL", response.raw().toString())
                    //magari aggiungi un toast
                    // Handle API error
                }
            } catch (e: Exception) {
                Log.d("CRASHVIEWMODEL", e.toString())
                //magari aggiungi un toast
                // Handle network or other errors
            }
        }
    }
}