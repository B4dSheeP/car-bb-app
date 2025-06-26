package com.macc.car_black_box.models

import com.google.gson.annotations.SerializedName
object CrashModels {
    data class CrashReport (
        @SerializedName("timestamp")
        val timestamp: Long,
        @SerializedName("accel_data")
        val accel_data: List<AccelData>,
        @SerializedName("gps_data")
        val gps_data: List<GpsData>,
    )

    data class AccelData (
        @SerializedName("instant")
        val instant: Long,
        @SerializedName("x")
        val x: Float,
        @SerializedName("y")
        val y: Float,
        @SerializedName("z")
        val z: Float,
    )


    data class GpsData(
        @SerializedName("instant")
        val instant: Long,
        @SerializedName("latitude")
        val latitude: Float,
        @SerializedName("longitude")
        val longitude: Float,
        @SerializedName("altitude")
        val altitude: Float,
        @SerializedName("speed")
        val speed: Float
    )
}