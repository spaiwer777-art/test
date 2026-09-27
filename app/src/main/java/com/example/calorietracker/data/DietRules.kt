package com.example.calorietracker.data

/** Target share of calories from protein / fat / carbs, in percent (sums to 100). */
data class MacroSplit(val protein: Int, val fat: Int, val carbs: Int) {
    companion object {
        val DEFAULT = MacroSplit(20, 30, 50)
    }
}

/** Macro goals the user typed in by hand, grams per day; they override diet/auto goals. */
data class MacroGrams(val protein: Double, val fat: Double, val carbs: Double) {
    val calories: Double get() = protein * 4 + fat * 9 + carbs * 4
}

val Diet.split: MacroSplit get() = MacroSplit(proteinPct, fatPct, carbsPct)

/**
 * Checks food names against a diet's "avoid" keywords.
 *
 * Keyword forms (comma-separated, lower case, ё written as е):
 * - `кури`       — any word starting with it (курица, куриная), not "курага";
 * - `сыр=`       — the exact word only (сыр, but not "сырая");
 * - `сельдь солен` — a phrase anywhere in the text.
 */
object DietRules {
    private fun normalize(text: String) = text.lowercase().replace('ё', 'е')

    fun keywords(diet: Diet): List<String> =
        diet.avoid.split(',').map { normalize(it.trim()) }.filter { it.isNotEmpty() }

    /** The first keyword that makes [text] unsuitable for [diet], or null if it fits. */
    fun conflict(diet: Diet, text: String): String? {
        val norm = normalize(text)
        val words = norm.split(Regex("[^а-яa-z0-9-]+")).filter { it.isNotEmpty() }
        return keywords(diet).firstOrNull { kw ->
            when {
                kw.endsWith("=") -> kw.dropLast(1) in words
                ' ' in kw -> norm.contains(kw)
                else -> words.any { it.startsWith(kw) }
            }
        }?.removeSuffix("=")
    }

    fun fits(diet: Diet, vararg texts: String): Boolean = texts.none { conflict(diet, it) != null }
}
