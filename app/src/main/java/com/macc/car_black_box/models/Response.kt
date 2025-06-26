package com.macc.car_black_box.models
import com.google.gson.annotations.SerializedName


data class Response<T>(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?,
)