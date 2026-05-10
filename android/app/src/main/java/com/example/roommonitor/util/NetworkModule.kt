package com.example.roommonitor.util

import com.example.roommonitor.data.RoomStatusApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.roommonitor.data.ProvisioningApi

object NetworkModule {
    private const val ESP32_PROVISIONING_BASE_URL = "http://192.168.4.1/"

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
            .newBuilder()
            .addHeader("x-api-key", Constants.API_KEY)
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val provisioningRetrofit = Retrofit.Builder()
        .baseUrl(ESP32_PROVISIONING_BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val roomStatusApi: RoomStatusApi = retrofit.create(RoomStatusApi::class.java)
    val provisioningApi: ProvisioningApi = provisioningRetrofit.create(ProvisioningApi::class.java)
}
