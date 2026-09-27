package com.example.calorietracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Where a food record came from; shown as a badge on the product card. */
enum class FoodSource(val label: String) {
    USER("Мой продукт"),
    BARCODE("Штрихкод · Open Food Facts"),
    ONLINE("Open Food Facts"),
    BUILTIN("Справочник РФ"),
    USDA("USDA (США)");

    /** Read-only reference data shipped with the app. */
    val isReference: Boolean get() = this == BUILTIN || this == USDA
}

/**
 * A food with nutrition values per 100 grams. Comes from manual entry, a
 * barcode / online lookup (Open Food Facts) or the built-in reference base.
 */
@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val barcode: String? = null,
    val brand: String? = null,
    val category: String? = null,
    @ColumnInfo(defaultValue = "USER") val source: FoodSource = FoodSource.USER,
    /** Lower-cased name: SQLite LIKE is case-insensitive only for ASCII, not Cyrillic. */
    @ColumnInfo(defaultValue = "") val searchName: String = name.lowercase(),
    val fiberPer100g: Double? = null,
    val sugarPer100g: Double? = null,
    val saturatedFatPer100g: Double? = null,
    val saltPer100g: Double? = null,
    val servingGrams: Double? = null,
    val servingLabel: String? = null,
    val nutriScore: String? = null
)
