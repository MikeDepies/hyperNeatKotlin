package algorithm.activation

import genome.ActivationFunction

interface ActivationFunctionSelection {
    fun select(): ActivationFunction
}

