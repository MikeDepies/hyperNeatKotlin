package algorithm.activation

import genome.ActivationFunction
import kotlin.random.Random

class RandomActivationFunctionSelection(private val random: Random, private val activationFunctions: List<ActivationFunction>) :
    ActivationFunctionSelection {
    override fun select(): ActivationFunction {
        return activationFunctions.random(random)
    }
}