package com.example.calorietracker.data

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Opens a v1 database (as shipped in 1.0.x) with the current Room schema and checks nothing is lost. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MigrationTest {
    private val v1Schema = listOf(
        "CREATE TABLE IF NOT EXISTS `foods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `caloriesPer100g` REAL NOT NULL, `proteinPer100g` REAL NOT NULL, `fatPer100g` REAL NOT NULL, `carbsPer100g` REAL NOT NULL, `barcode` TEXT)",
        "CREATE TABLE IF NOT EXISTS `diary_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `foodName` TEXT NOT NULL, `grams` REAL NOT NULL, `calories` REAL NOT NULL, `protein` REAL NOT NULL, `fat` REAL NOT NULL, `carbs` REAL NOT NULL, `mealType` TEXT NOT NULL, `epochDay` INTEGER NOT NULL)"
    )

    @Test
    fun v1DataSurvivesMigration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) = v1Schema.forEach(db::execSQL)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        helper.writableDatabase.apply {
            insert("foods", SQLiteDatabase.CONFLICT_NONE, ContentValues().apply {
                put("name", "Майонез Ряба"); put("caloriesPer100g", 620.0); put("proteinPer100g", 0.5)
                put("fatPer100g", 67.0); put("carbsPer100g", 2.5); put("barcode", "4600528347265")
            })
            insert("diary_entries", SQLiteDatabase.CONFLICT_NONE, ContentValues().apply {
                put("foodName", "Гречка"); put("grams", 200.0); put("calories", 220.0); put("protein", 8.4)
                put("fat", 2.2); put("carbs", 42.6); put("mealType", "LUNCH"); put("epochDay", 20000L)
            })
        }
        helper.close()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            val entries = db.diaryDao().getForDay(20000L).first()
            assertEquals(1, entries.size)
            assertEquals("Гречка", entries[0].foodName)
            assertEquals(null, entries[0].foodId)

            val food = db.foodDao().findByBarcode("4600528347265")
            assertNotNull(food)
            assertEquals(FoodSource.BARCODE, food!!.source)
            assertEquals("", food.searchName) // backfilled by Seeder at startup

            // New tables are usable.
            db.trackingDao().upsertWater(WaterEntry(20000L, 750))
            assertEquals(750, db.trackingDao().water(20000L).first()?.ml)
            db.dietDao().upsert(Diet(0, "Тест", "", 30, 30, 40, -10, 3, "", "сахар=", false))
            assertEquals(1, db.dietDao().all().first().size)
        }
        db.close()
    }
}
