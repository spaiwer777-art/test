package com.example.calorietracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Sum of all diary entries for one day. */
data class DayTotals(
    val epochDay: Long,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double
)

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE epochDay = :epochDay ORDER BY id ASC")
    fun getForDay(epochDay: Long): Flow<List<DiaryEntry>>

    @Insert
    suspend fun insert(entry: DiaryEntry): Long

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT DISTINCT epochDay FROM diary_entries ORDER BY epochDay DESC")
    fun getDaysWithEntries(): Flow<List<Long>>

    @Query(
        """
        SELECT epochDay, SUM(calories) AS calories, SUM(protein) AS protein,
               SUM(fat) AS fat, SUM(carbs) AS carbs
        FROM diary_entries
        WHERE epochDay BETWEEN :fromDay AND :toDay
        GROUP BY epochDay
        ORDER BY epochDay ASC
        """
    )
    fun dailyTotals(fromDay: Long, toDay: Long): Flow<List<DayTotals>>
}
