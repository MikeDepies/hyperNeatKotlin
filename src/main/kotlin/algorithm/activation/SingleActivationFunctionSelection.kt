package algorithm.activation

import genome.ActivationFunction

class SingleActivationFunctionSelection(val activationFunction: ActivationFunction) : ActivationFunctionSelection {
    override fun select(): ActivationFunction {
        return activationFunction
    }
}