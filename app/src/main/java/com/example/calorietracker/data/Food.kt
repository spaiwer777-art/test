package com.example.calorietracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A food item the user has added to their personal library, with nutrition
 * values per 100 grams. Can come from manual entry or a barcode lookup
 * (Open Food Facts).
 */
@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val barcode: String? = null
)
