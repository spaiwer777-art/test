package com.example.calorietracker.data

/** Matching and ranking for the product search (pure functions, unit-tested). */
object FoodSearch {
    fun normalize(text: String) = text.lowercase().replace('ё', 'е')

    private fun words(text: String) = normalize(text).split(Regex("[^а-яa-z0-9%]+")).filter { it.isNotEmpty() }

    /** Word stems: drop the last two letters of longer words so endings don't matter (огурцы/огурец). */
    fun stems(query: String): List<String> = words(query).map { w ->
        if (w.length >= 5) w.take(maxOf(4, w.length - 2)) else w
    }

    /** SQL LIKE pattern for a stem; "е" becomes "_" so stored "ё" still matches. */
    fun likePattern(stem: String) = "%" + stem.replace('е', '_') + "%"

    fun matches(stems: List<String>, name: String): Boolean {
        val nameWords = words(name)
        return stems.all { s -> nameWords.any { it.startsWith(s) } }
    }

    /**
     * Keeps names containing every stem at a word start and orders them: user
     * products, then RU reference, then USDA; names starting with the query's
     * first word first; shorter names first.
     */
    fun rank(query: String, stems: List<String>, foods: List<Food>): List<Food> {
        val first = stems.first()
        return foods.filter { matches(stems, it.name) }.sortedWith(
            compareBy<Food>(
                { when (it.source) { FoodSource.BUILTIN -> 1; FoodSource.USDA -> 2; else -> 0 } },
                { if (words(it.name).firstOrNull()?.startsWith(first) == true) 0 else 1 },
                { it.name.length },
                { it.name }
            )
        )
    }
}
