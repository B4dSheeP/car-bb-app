package com.macc.car_black_box

import com.macc.car_black_box.models.AuthenticationModels
import com.macc.car_black_box.models.CrashModels
import com.macc.car_black_box.models.Response

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.GET


interface CrashApiService{
    @Headers("Content-Type: application/json")
    @POST("/signup")
    fun signup(@Body signup_data: AuthenticationModels.SignupRequest): Call<Response<String>>

    @Headers("Content-Type: application/json")
    @POST("/signin")
    fun signin(@Body signin_data: AuthenticationModels.SigninRequest): Call<Response<AuthenticationModels.SigninResponse>>

    @Headers("Content-Type: application/json")
    @POST("/crashes/new")
    fun new_crash(@Body crash_data: CrashModels.CrashReport): Call<Response<String>>

    @Headers("Content-Type: application/json")
    @GET("/crashes/all")
    fun get_crashes(): Call<Response<List<CrashModels.CrashReport>>>
}