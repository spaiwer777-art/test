package com.example.calorietracker.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val client = OkHttpClient.Builder()
        // Open Food Facts asks apps to identify themselves.
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", "CalorieTracker/1.0 (Android)").build())
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val openFoodFactsApi: OpenFoodFactsApi by lazy {
        retrofit("https://world.openfoodfacts.org/").create(OpenFoodFactsApi::class.java)
    }

    val offSearchApi: OffSearchApi by lazy {
        retrofit("https://search.openfoodfacts.org/").create(OffSearchApi::class.java)
    }

    val groqApi: GroqApi by lazy {
        retrofit("https://api.groq.com/").create(GroqApi::class.java)
    }
}
