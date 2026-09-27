package com.example.calorietracker.network

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * TheMealDB (https://www.themealdb.com): free recipe database with photos.
 * Uses the public test key "1", which the site allows for development and
 * personal/educational use; a public app-store release needs a supporter key.
 * Responses are read as JsonObject because each meal has 20 numbered
 * ingredient/measure fields.
 */
interface MealDbApi {
    @GET("search.php") suspend fun search(@Query("s") query: String): JsonObject
    @GET("filter.php") suspend fun byCategory(@Query("c") category: String): JsonObject
    @GET("filter.php") suspend fun byArea(@Query("a") area: String): JsonObject
    @GET("lookup.php") suspend fun lookup(@Query("i") id: String): JsonObject
}

/** Short card for lists. */
data class MealSummary(val id: String, val name: String, val thumb: String?)

data class MealIngredient(val name: String, val measure: String)

data class MealDetails(
    val id: String,
    val name: String,
    val category: String?,
    val area: String?,
    val instructions: String,
    val thumb: String?,
    val youtube: String?,
    val ingredients: List<MealIngredient>
)

fun JsonObject.meals(): List<JsonObject> =
    get("meals")?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asJsonObject }.orEmpty()

private fun JsonObject.str(key: String): String? =
    get(key)?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotEmpty() }

fun JsonObject.toSummary() = MealSummary(str("idMeal").orEmpty(), str("strMeal").orEmpty(), str("strMealThumb"))

fun JsonObject.toDetails() = MealDetails(
    id = str("idMeal").orEmpty(),
    name = str("strMeal").orEmpty(),
    category = str("strCategory"),
    area = str("strArea"),
    instructions = str("strInstructions").orEmpty(),
    thumb = str("strMealThumb"),
    youtube = str("strYoutube"),
    ingredients = (1..20).mapNotNull { i ->
        str("strIngredient$i")?.let { MealIngredient(it, str("strMeasure$i").orEmpty()) }
    }
)
