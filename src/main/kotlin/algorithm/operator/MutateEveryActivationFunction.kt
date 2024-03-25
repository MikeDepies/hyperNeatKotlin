package algorithm.operator

import genome.ActivationFunction
import genome.NetworkGenome
import kotlin.random.Random
interface MutateActivationFunctionOperator : GeneticOperator {
    val random: Random
    val activationFunctions: List<ActivationFunction>
}
class MutateEveryActivationFunction(
    override val random: Random,
    override val activationFunctions: List<ActivationFunction>
) : MutateActivationFunctionOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        val updatedNodeGenomes =
                genome.nodeGenomes.map { it.copy(activationFunction = randomActivationFunction()) }
        return genome.copy(nodeGenomes = updatedNodeGenomes)
    }
    fun randomActivationFunction(): ActivationFunction {
        return activationFunctions.random(random)
    }
}
class MutateRandomActivationFunction(
    override val random: Random,
    override val activationFunctions: List<ActivationFunction>
) : MutateActivationFunctionOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        if (genome.nodeGenomes.isEmpty()) return genome
        val randomIndex = random.nextInt(genome.nodeGenomes.size)
        val updatedNodeGenomes = genome.nodeGenomes.mapIndexed { index, nodeGenome ->
            if (index == randomIndex) nodeGenome.copy(activationFunction = randomActivationFunction())
            else nodeGenome
        }
        return genome.copy(nodeGenomes = updatedNodeGenomes)
    }
    fun randomActivationFunction(): ActivationFunction {
        return activationFunctions.random(random)
    }
}
