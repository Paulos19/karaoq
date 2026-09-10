package com.karaoq.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // URL padrão: com 'adb reverse tcp:8000 tcp:8000', http://localhost:8000/ funciona direto via USB!
    var currentBaseUrl: String = "http://localhost:8000/"
        private set

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private var retrofit: Retrofit = buildRetrofit(currentBaseUrl)
    var apiService: KaraoqApiService = retrofit.create(KaraoqApiService::class.java)
        private set

    private fun buildRetrofit(url: String): Retrofit {
        val safeUrl = if (url.endsWith("/")) url else "$url/"
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun updateBaseUrl(newUrl: String) {
        currentBaseUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        retrofit = buildRetrofit(currentBaseUrl)
        apiService = retrofit.create(KaraoqApiService::class.java)
    }
}
