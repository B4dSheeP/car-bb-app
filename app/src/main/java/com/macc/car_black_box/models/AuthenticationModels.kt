package com.macc.car_black_box.models
import com.google.gson.annotations.SerializedName

object AuthenticationModels {
    data class SignupRequest(
        @SerializedName("username")
        val userName: String,
        @SerializedName("password")
        val password: String,
        @SerializedName("password2")
        val password2: String
    )

    data class SigninRequest(
        @SerializedName("username")
        val userName: String,
        @SerializedName("password")
        val password: String
    )

    data class SigninResponse(
        @SerializedName("token")
        val token: String
    )

}