package com.example.calorietracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
}
