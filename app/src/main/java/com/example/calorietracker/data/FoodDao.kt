package com.example.calorietracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY CASE source WHEN 'BUILTIN' THEN 1 ELSE 0 END, name ASC")
    fun getAll(): Flow<List<Food>>

    /** User's own and scanned products first, then the reference base. */
    @Query(
        """
        SELECT * FROM foods WHERE searchName LIKE '%' || :query || '%'
        ORDER BY CASE source WHEN 'BUILTIN' THEN 1 ELSE 0 END,
                 CASE WHEN searchName LIKE :query || '%' THEN 0 ELSE 1 END,
                 length(name), name
        LIMIT 100
        """
    )
    fun search(query: String): Flow<List<Food>>

    @Query("SELECT * FROM foods WHERE source = 'BUILTIN'")
    suspend fun builtin(): List<Food>

    @Query("SELECT * FROM foods WHERE id = :id")
    fun observe(id: Long): Flow<Food?>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun get(id: Long): Food?

    @Query("SELECT * FROM foods WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): Food?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: Food): Long

    @Insert
    suspend fun insertAll(foods: List<Food>)

    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM foods WHERE source = 'BUILTIN'")
    suspend fun deleteBuiltin()

    @Query("SELECT id, name FROM foods WHERE searchName = ''")
    suspend fun missingSearchName(): List<IdName>

    @Query("UPDATE foods SET searchName = :searchName WHERE id = :id")
    suspend fun setSearchName(id: Long, searchName: String)
}

data class IdName(val id: Long, val name: String)
