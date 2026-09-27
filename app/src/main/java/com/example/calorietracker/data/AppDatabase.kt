package com.example.calorietracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter
    fun fromFoodSource(value: FoodSource): String = value.name

    @TypeConverter
    fun toFoodSource(value: String): FoodSource =
        FoodSource.entries.firstOrNull { it.name == value } ?: FoodSource.USER
}

@Database(
    entities = [
        Food::class, DiaryEntry::class, Recipe::class, RecipeIngredient::class,
        MealPhoto::class, WeightEntry::class, WaterEntry::class, MealPlanEntity::class, Diet::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun diaryDao(): DiaryDao
    abstract fun recipeDao(): RecipeDao
    abstract fun trackingDao(): TrackingDao
    abstract fun dietDao(): DietDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calorie_tracker.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}

/** v1 -> v2: richer foods, links from diary entries, recipes, photos, weight, water, plans. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_2_SQL.forEach(db::execSQL)
    }
}

/** v2 -> v3: diets and cooked weight for recipes. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_2_3_SQL.forEach(db::execSQL)
    }
}
