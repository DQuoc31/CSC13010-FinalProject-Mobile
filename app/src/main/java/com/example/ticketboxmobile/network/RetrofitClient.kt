package com.example.ticketboxmobile.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import com.example.ticketboxmobile.BuildConfig

object RetrofitClient {
    // URL is now hidden in local.properties and injected via BuildConfig
    private val BASE_URL = BuildConfig.BASE_URL

    val instance: AuthApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(AuthApi::class.java)
    }

    val mobileApi: MobileApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(MobileApi::class.java)
    }
}
