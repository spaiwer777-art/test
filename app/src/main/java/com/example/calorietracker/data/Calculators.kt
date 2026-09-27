package com.example.calorietracker.data

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** Pure formulas behind the calculators screen. Heights in cm, weights in kg. */
object Calc {
    /** Basal metabolic rate, Mifflin–St Jeor. */
    fun bmr(p: Profile): Double =
        10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + if (p.sex == Sex.MALE) 5 else -161

    /** Total daily energy expenditure. */
    fun tdee(p: Profile): Double = bmr(p) * p.activity.factor

    /** Daily calorie target for the profile's goal, never below a safe floor. */
    fun calorieTarget(p: Profile): Double {
        val floor = if (p.sex == Sex.MALE) 1500.0 else 1200.0
        return maxOf(tdee(p) * p.goal.factor, floor)
    }

    fun bmi(weightKg: Double, heightCm: Double): Double = weightKg / (heightCm / 100).pow(2)

    enum class BmiClass(val label: String, val upper: Double) {
        UNDER("Недостаток веса", 18.5),
        NORMAL("Норма", 25.0),
        OVER("Избыточный вес", 30.0),
        OBESE("Ожирение", Double.MAX_VALUE)
    }

    fun bmiClass(bmi: Double): BmiClass = BmiClass.entries.first { bmi < it.upper }

    /** Healthy weight range for the height (BMI 18.5–24.9). */
    fun healthyWeightRange(heightCm: Double): ClosedFloatingPointRange<Double> {
        val h2 = (heightCm / 100).pow(2)
        return 18.5 * h2..24.9 * h2
    }

    /** Daily water need: 30 ml per kg plus extra for training, rounded to 50 ml. */
    fun waterMl(weightKg: Double, activity: ActivityLevel): Int {
        val extra = when (activity) {
            ActivityLevel.SEDENTARY, ActivityLevel.LIGHT -> 0
            ActivityLevel.MODERATE -> 300
            ActivityLevel.HIGH -> 500
            ActivityLevel.EXTREME -> 700
        }
        return (((weightKg * 30 + extra) / 50).roundToInt() * 50)
    }

    data class Macros(val protein: Double, val fat: Double, val carbs: Double)

    /** Protein and fat by body weight, carbs fill the rest of the calorie target. */
    fun macros(p: Profile, calories: Double = calorieTarget(p)): Macros {
        val proteinPerKg = when (p.goal) {
            WeightGoal.LOSE -> 2.0
            WeightGoal.MAINTAIN -> 1.6
            WeightGoal.GAIN -> 1.8
        }
        val protein = proteinPerKg * p.weightKg
        val fat = maxOf(0.9 * p.weightKg, calories * 0.2 / 9)
        val carbs = maxOf(0.0, (calories - protein * 4 - fat * 9) / 4)
        return Macros(protein, fat, carbs)
    }

    /** Body fat %, U.S. Navy method (tape measurements in cm). Null if measurements are impossible. */
    fun bodyFatNavy(sex: Sex, heightCm: Double, neckCm: Double, waistCm: Double, hipCm: Double): Double? {
        val result = if (sex == Sex.MALE) {
            if (waistCm <= neckCm) return null
            495 / (1.0324 - 0.19077 * log10(waistCm - neckCm) + 0.15456 * log10(heightCm)) - 450
        } else {
            if (waistCm + hipCm <= neckCm) return null
            495 / (1.29579 - 0.35004 * log10(waistCm + hipCm - neckCm) + 0.22100 * log10(heightCm)) - 450
        }
        return result.takeIf { it in 2.0..70.0 }
    }

    /** Classic "ideal weight" formulas; all of them are rough population averages. */
    fun idealWeights(sex: Sex, heightCm: Double): List<Pair<String, Double>> {
        val over = heightCm - 152.4
        val male = sex == Sex.MALE
        return listOf(
            "Девайн" to (if (male) 50.0 else 45.5) + 0.9055 * over,
            "Робинсон" to (if (male) 52.0 else 49.0) + (if (male) 0.748 else 0.669) * over,
            "Миллер" to (if (male) 56.2 else 53.1) + (if (male) 0.555 else 0.535) * over
        )
    }

    data class Exercise(val name: String, val met: Double)

    val exercises = listOf(
        Exercise("Ходьба, 5 км/ч", 3.5),
        Exercise("Быстрая ходьба, 6,5 км/ч", 5.0),
        Exercise("Бег, 8 км/ч", 8.3),
        Exercise("Бег, 10 км/ч", 9.8),
        Exercise("Велосипед, 16–19 км/ч", 6.8),
        Exercise("Плавание", 5.8),
        Exercise("Силовая тренировка", 5.0),
        Exercise("Интервальная (HIIT)", 8.0),
        Exercise("Йога", 2.5),
        Exercise("Танцы", 5.0),
        Exercise("Футбол", 7.0),
        Exercise("Уборка дома", 3.3)
    )

    /** Calories burned: MET × weight × hours. */
    fun exerciseKcal(met: Double, weightKg: Double, minutes: Double): Double = met * weightKg * minutes / 60
}
