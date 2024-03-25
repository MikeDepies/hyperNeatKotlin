package algorithm.weight

import kotlin.random.Random

class SimpleRandomWeight(val random: Random, val weightRange: ClosedRange<Double>) : RandomWeight {
    override operator fun invoke(): Double {
        return (random.nextDouble() * (weightRange.endInclusive - weightRange.start) +
                weightRange.start)
    }
}