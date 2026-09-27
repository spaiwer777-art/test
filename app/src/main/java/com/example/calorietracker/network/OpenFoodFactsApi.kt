package com.example.calorietracker.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Open Food Facts is a free, open, crowd-sourced food database — no API key
 * needed. We use it to look up a product's nutrition facts by barcode.
 */
data class OffProduct(
    val product_name: String?,
    val nutriments: OffNutriments?
)

data class OffNutriments(
    @SerializedName("energy-kcal_100g") val energyKcal100g: Double?,
    val proteins_100g: Double?,
    val fat_100g: Double?,
    val carbohydrates_100g: Double?
)

data class OffResponse(
    val status: Int,
    val product: OffProduct?
)

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OffResponse
}
