package com.example.calorietracker.network

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Open Food Facts is a free, open, crowd-sourced food database — no API key
 * needed. We look products up by barcode and search them by name.
 */
data class OffNutriments(
    @SerializedName("energy-kcal_100g") val energyKcal100g: Double?,
    @SerializedName("proteins_100g") val proteins100g: Double?,
    @SerializedName("fat_100g") val fat100g: Double?,
    @SerializedName("carbohydrates_100g") val carbohydrates100g: Double?,
    @SerializedName("fiber_100g") val fiber100g: Double?,
    @SerializedName("sugars_100g") val sugars100g: Double?,
    @SerializedName("saturated-fat_100g") val saturatedFat100g: Double?,
    @SerializedName("salt_100g") val salt100g: Double?
)

data class OffProduct(
    val code: String?,
    @SerializedName("product_name") val productName: String?,
    @SerializedName("product_name_ru") val productNameRu: String?,
    /** A string in the product API, a list in the search API. */
    val brands: JsonElement?,
    @SerializedName("nutriscore_grade") val nutriscoreGrade: String?,
    @SerializedName("serving_quantity") val servingQuantity: JsonElement?,
    val nutriments: OffNutriments?
) {
    val displayName: String? get() = (productNameRu ?: productName)?.trim()?.takeIf { it.isNotEmpty() }
    val brandText: String?
        get() = when {
            brands == null || brands.isJsonNull -> null
            brands.isJsonArray -> brands.asJsonArray.firstOrNull()?.asString
            else -> brands.asString.split(',').firstOrNull()?.trim()
        }?.takeIf { it.isNotBlank() }
    val servingGrams: Double?
        get() = runCatching { servingQuantity?.asDouble }.getOrNull()?.takeIf { it > 0 }
}

data class OffResponse(val status: Int, val product: OffProduct?)

data class OffSearchResponse(val hits: List<OffProduct>?)

const val OFF_FIELDS =
    "code,product_name,product_name_ru,brands,nutriscore_grade,serving_quantity,nutriments"

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = OFF_FIELDS
    ): OffResponse
}

/** Search-a-licious: Open Food Facts' full-text search service. */
interface OffSearchApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page_size") pageSize: Int = 25,
        @Query("langs") langs: String = "ru",
        @Query("fields") fields: String = OFF_FIELDS
    ): OffSearchResponse
}
