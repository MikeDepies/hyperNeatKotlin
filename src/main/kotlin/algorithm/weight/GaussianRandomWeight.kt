package algorithm.weight

import kotlin.random.Random

class GaussianRandomWeight(
    val random: Random,
    val mean: Double,
    val stdDev: Double,
    val min: Double = Double.MIN_VALUE,
    val max: Double = Double.MAX_VALUE
) : RandomWeight {
    override operator fun invoke(): Double {
        var gaussianValue: Double
        do {
            gaussianValue = random.nextGaussian() * stdDev + mean
        } while (gaussianValue < min || gaussianValue > max)
        return gaussianValue
    }
    fun Random.nextGaussian(
            mean: Double = 0.0,
            stdDev: Double = 1.0,
            min: Double = Double.MIN_VALUE,
            max: Double = Double.MAX_VALUE
    ): Double {
        var gaussianValue: Double
        do {
            // Box-Muller transform
            var u1: Double
            var u2: Double
            var w: Double
            var mult: Double
            do {
                u1 = 2 * this.nextDouble() - 1 // between -1.0 and 1.0
                u2 = 2 * this.nextDouble() - 1 // between -1.0 and 1.0
                w = u1 * u1 + u2 * u2
            } while (w >= 1 || w == 0.0)

            mult = kotlin.math.sqrt((-2 * kotlin.math.ln(w)) / w)
            gaussianValue = mean + stdDev * u1 * mult
        } while (gaussianValue < min || gaussianValue > max)
        return gaussianValue
    }
}