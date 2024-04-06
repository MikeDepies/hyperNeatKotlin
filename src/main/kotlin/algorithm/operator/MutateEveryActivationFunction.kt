package algorithm.operator

import genome.ActivationFunction
import genome.NetworkGenome
import kotlin.random.Random
import algorithm.activation.ActivationFunctionSelection
interface MutateActivationFunctionOperator : GeneticOperator {
    val random: Random
    val activationSelection : ActivationFunctionSelection
}
class MutateEveryActivationFunction(
    override val random: Random,
    override val activationSelection: ActivationFunctionSelection
) : MutateActivationFunctionOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        val updatedNodeGenomes =
                genome.nodeGenomes.map { it.copy(activationFunction = activationSelection.select()) }
        return genome.copy(nodeGenomes = updatedNodeGenomes,
            fitness = null,
            sharedFitness = null,
            speciesId = null)
    }
}
class MutateRandomActivationFunction(
    override val random: Random,
    override val activationSelection: ActivationFunctionSelection
) : MutateActivationFunctionOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        if (genome.nodeGenomes.isEmpty()) return genome
        val randomIndex = random.nextInt(genome.nodeGenomes.size)
        val updatedNodeGenomes = genome.nodeGenomes.mapIndexed { index, nodeGenome ->
            if (index == randomIndex) nodeGenome.copy(activationFunction = activationSelection.select())
            else nodeGenome
        }
        return genome.copy(nodeGenomes = updatedNodeGenomes,
            fitness = null,
            sharedFitness = null,
            speciesId = null)
    }
    
}
