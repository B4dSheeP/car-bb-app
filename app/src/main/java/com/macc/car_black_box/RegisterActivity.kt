// RegisterActivity.kt
package com.macc.car_black_box

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Retrofit

import com.macc.car_black_box.CrashApiService
import com.macc.car_black_box.CrashApiConstants
import com.macc.car_black_box.models.AuthenticationModels.SignupRequest
import com.macc.car_black_box.models.Response
import retrofit2.Call
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.registerEmail).text.toString()
            val password = findViewById<EditText>(R.id.registerPassword).text.toString()
            val confirm = findViewById<EditText>(R.id.registerConfirmPassword).text.toString()

            if (email.isNotBlank() && password == confirm) {
                remoteRegister(email, password, confirm)

            } else {
                Toast.makeText(this, "Errore nella registrazione", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun remoteRegister(email: String, password: String, confirm: String){
        val retrofit = Retrofit.Builder()
            .baseUrl(CrashApiConstants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(CrashApiService::class.java)
        val call = apiService.signup(SignupRequest(email, password, confirm))
        call.enqueue(object : Callback<Response<String>> {
            override fun onResponse(
                call: Call<Response<String>>,
                response: retrofit2.Response<Response<String>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val resp_obj = response.body()!!
                    if (resp_obj.status == "ok") {
                        Toast.makeText(this@RegisterActivity, resp_obj.data, Toast.LENGTH_LONG).show()
                        this@RegisterActivity.finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, resp_obj.message, Toast.LENGTH_LONG).show()
                    }
                }
                else{
                    Toast.makeText(this@RegisterActivity, "Status code ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Response<String>>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "Failed to connect to the remote server", Toast.LENGTH_LONG).show()
                t.printStackTrace()
            }
        })

    }
}
