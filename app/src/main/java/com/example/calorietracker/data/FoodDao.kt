package com.example.calorietracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query(
        """
        SELECT * FROM foods WHERE source IN (:sources)
        ORDER BY CASE source WHEN 'BUILTIN' THEN 1 WHEN 'USDA' THEN 2 ELSE 0 END, name ASC
        LIMIT 5000
        """
    )
    fun getAll(sources: List<FoodSource>): Flow<List<Food>>

    /** User's own and scanned products first, then the RU reference base, then USDA. */
    @Query(
        """
        SELECT * FROM foods WHERE searchName LIKE '%' || :query || '%' AND source IN (:sources)
        ORDER BY CASE source WHEN 'BUILTIN' THEN 1 WHEN 'USDA' THEN 2 ELSE 0 END,
                 CASE WHEN searchName LIKE :query || '%' THEN 0 ELSE 1 END,
                 length(name), name
        LIMIT 500
        """
    )
    fun search(query: String, sources: List<FoodSource>): Flow<List<Food>>

    /**
     * Candidates for a multi-word search: rows whose searchName matches [pattern]
     * (a LIKE pattern built from the most specific word). The remaining words are
     * checked in Kotlin, see FoodRepository.searchFoods.
     */
    @Query("SELECT * FROM foods WHERE searchName LIKE :pattern AND source IN (:sources) LIMIT 1000")
    suspend fun candidates(pattern: String, sources: List<FoodSource>): List<Food>

    /** Emits whenever the foods table changes, to re-run searches. */
    @Query("SELECT COUNT(*) FROM foods")
    fun changes(): Flow<Int>

    /** Reference bases, RU first: used to ground AI ingredient estimates. */
    @Query("SELECT * FROM foods WHERE source IN ('BUILTIN', 'USDA') ORDER BY CASE source WHEN 'BUILTIN' THEN 0 ELSE 1 END, id")
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

    @Query("DELETE FROM foods WHERE source IN ('BUILTIN', 'USDA')")
    suspend fun deleteBuiltin()

    @Query("SELECT id, name FROM foods WHERE source IN ('BUILTIN', 'USDA')")
    suspend fun builtinNames(): List<IdName>

    @Query("UPDATE diary_entries SET foodId = :newId WHERE foodId = :oldId")
    suspend fun remapDiaryFood(oldId: Long, newId: Long?)

    @Query("SELECT id, name FROM foods WHERE searchName = ''")
    suspend fun missingSearchName(): List<IdName>

    @Query("UPDATE foods SET searchName = :searchName WHERE id = :id")
    suspend fun setSearchName(id: Long, searchName: String)
}

data class IdName(val id: Long, val name: String)
