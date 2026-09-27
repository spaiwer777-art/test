package com.example.calorietracker.data

/**
 * Matches free-text ingredient names from the AI ("гречка (варёная)",
 * "масло сливочное") to entries of the built-in reference base, so we can use
 * the reference nutrition values instead of the model's guesses.
 *
 * Words are compared by a 4-letter stem (Russian endings vary: огурец/огурцы).
 * The first word must match, and the cooking state (raw/boiled/fried/dry/baked)
 * must not contradict: "гречка сухая" and "гречка варёная" differ threefold.
 */
object FoodMatcher {
    private val stateGroups = mapOf(
        "raw" to setOf("сыра", "сыро", "сыры"),
        "boiled" to setOf("варе", "отва"),
        "fried" to setOf("жаре"),
        "dry" to setOf("суха", "сухо", "сухи", "сухо"),
        "baked" to setOf("запе")
    )

    private fun words(text: String): List<String> =
        text.lowercase().replace('ё', 'е')
            .split(Regex("[^а-яa-z]+"))
            .filter { it.length >= 3 }

    private fun stem(word: String) = word.take(4)

    private fun states(stems: Collection<String>): Set<String> =
        stateGroups.filterValues { group -> stems.any { it in group } }.keys

    fun match(itemName: String, candidates: List<Food>): Food? {
        val itemStems = words(itemName).map(::stem)
        val first = itemStems.firstOrNull() ?: return null
        val itemState = states(itemStems)

        data class Scored(val food: Food, val matched: Int, val extra: Int, val stateless: Boolean)

        val scored = candidates.mapNotNull { food ->
            val baseStems = words(food.name).map(::stem)
            if (baseStems.firstOrNull() != first) return@mapNotNull null
            val baseState = states(baseStems)
            if (itemState.isNotEmpty() && baseState.isNotEmpty() && baseState != itemState) return@mapNotNull null
            val matched = itemStems.count { it in baseStems }
            Scored(food, matched, baseStems.size - matched, baseState.isEmpty())
        }
        if (scored.isEmpty()) return null
        // Grains and pasta differ ~3x dry vs cooked: without a stated state, don't guess.
        if (itemState.isEmpty() && scored.any { "dry" in states(words(it.food.name).map(::stem)) }) return null
        // Without a stated cooking state, only trust an unambiguous stateless match.
        val pool = if (itemState.isEmpty()) scored.filter { it.stateless }.ifEmpty {
            if (scored.size == 1) scored else return null
        } else scored
        return pool.sortedWith(compareByDescending<Scored> { it.matched }.thenBy { it.extra }).first().food
    }
}
