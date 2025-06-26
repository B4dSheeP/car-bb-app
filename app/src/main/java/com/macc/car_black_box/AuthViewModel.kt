package com.macc.car_black_box

import androidx.lifecycle.ViewModel
import okhttp3.OkHttpClient

class AuthViewModel(jwtToken: String) : ViewModel() {
    var jwtToken: String = jwtToken

    fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                requestBuilder.header("Authorization", "Bearer ${jwtToken}")
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .build()
    }
}
