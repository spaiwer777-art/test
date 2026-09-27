package com.example.calorietracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name ASC")
    fun getAll(): Flow<List<Food>>

    @Query("SELECT * FROM foods WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<Food>>

    @Query("SELECT * FROM foods WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): Food?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: Food): Long

    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun delete(id: Long)
}
