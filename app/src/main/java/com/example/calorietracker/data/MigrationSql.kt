package com.example.calorietracker.data

/**
 * Statements for MIGRATION_1_2, derived from the exported Room schema
 * (app/schemas/.../2.json) so they match what Room validates on open.
 * Existing rows get searchName = '' and are backfilled in Kotlin at startup
 * (SQLite's lower() only handles ASCII, not Cyrillic).
 */
internal val MIGRATION_1_2_SQL = listOf(
    "ALTER TABLE `foods` ADD COLUMN `brand` TEXT",
    "ALTER TABLE `foods` ADD COLUMN `category` TEXT",
    "ALTER TABLE `foods` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'USER'",
    "ALTER TABLE `foods` ADD COLUMN `searchName` TEXT NOT NULL DEFAULT ''",
    "ALTER TABLE `foods` ADD COLUMN `fiberPer100g` REAL",
    "ALTER TABLE `foods` ADD COLUMN `sugarPer100g` REAL",
    "ALTER TABLE `foods` ADD COLUMN `saturatedFatPer100g` REAL",
    "ALTER TABLE `foods` ADD COLUMN `saltPer100g` REAL",
    "ALTER TABLE `foods` ADD COLUMN `servingGrams` REAL",
    "ALTER TABLE `foods` ADD COLUMN `servingLabel` TEXT",
    "ALTER TABLE `foods` ADD COLUMN `nutriScore` TEXT",
    "ALTER TABLE `diary_entries` ADD COLUMN `foodId` INTEGER",
    "ALTER TABLE `diary_entries` ADD COLUMN `recipeId` INTEGER",
    "UPDATE `foods` SET `source` = 'BARCODE' WHERE `barcode` IS NOT NULL",
    "CREATE TABLE IF NOT EXISTS `recipes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `servings` INTEGER NOT NULL, `minutes` INTEGER NOT NULL, `steps` TEXT NOT NULL, `isBuiltin` INTEGER NOT NULL, `caloriesPerServing` REAL NOT NULL, `proteinPerServing` REAL NOT NULL, `fatPerServing` REAL NOT NULL, `carbsPerServing` REAL NOT NULL)",
    "CREATE TABLE IF NOT EXISTS `recipe_ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipeId` INTEGER NOT NULL, `name` TEXT NOT NULL, `grams` REAL NOT NULL, `caloriesPer100g` REAL NOT NULL, `proteinPer100g` REAL NOT NULL, `fatPer100g` REAL NOT NULL, `carbsPer100g` REAL NOT NULL)",
    "CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipeId` ON `recipe_ingredients` (`recipeId`)",
    "CREATE TABLE IF NOT EXISTS `meal_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `mealType` TEXT NOT NULL, `path` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
    "CREATE INDEX IF NOT EXISTS `index_meal_photos_epochDay` ON `meal_photos` (`epochDay`)",
    "CREATE TABLE IF NOT EXISTS `weight_entries` (`epochDay` INTEGER NOT NULL, `kg` REAL NOT NULL, PRIMARY KEY(`epochDay`))",
    "CREATE TABLE IF NOT EXISTS `water_entries` (`epochDay` INTEGER NOT NULL, `ml` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))",
    "CREATE TABLE IF NOT EXISTS `meal_plans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` INTEGER NOT NULL, `title` TEXT NOT NULL, `json` TEXT NOT NULL)",
)

/** Statements for MIGRATION_2_3, derived from the exported schema 3.json. */
internal val MIGRATION_2_3_SQL = listOf(
    "ALTER TABLE `recipes` ADD COLUMN `cookedWeight` REAL",
    "CREATE TABLE IF NOT EXISTS `diets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `proteinPct` INTEGER NOT NULL, `fatPct` INTEGER NOT NULL, `carbsPct` INTEGER NOT NULL, `calorieAdjustPct` INTEGER NOT NULL, `mealsPerDay` INTEGER NOT NULL, `recommended` TEXT NOT NULL, `avoid` TEXT NOT NULL, `isBuiltin` INTEGER NOT NULL)",
)

/** Statements for MIGRATION_3_4, derived from the exported schema 4.json. */
internal val MIGRATION_3_4_SQL = listOf(
    "ALTER TABLE `recipes` ADD COLUMN `imageUrl` TEXT",
    "ALTER TABLE `recipes` ADD COLUMN `externalId` TEXT",
)
