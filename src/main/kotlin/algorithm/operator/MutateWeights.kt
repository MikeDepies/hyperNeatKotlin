package algorithm.operator

import algorithm.weight.RandomWeight
import genome.NetworkGenome
interface MutateWeightsOperator : GeneticOperator {
    val randomWeight: RandomWeight
}
class MutateWeightsOperatorImpl(override val randomWeight: RandomWeight) : MutateWeightsOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        val updatedConnectionGenes = genome.connectionGenes.map { it.copy(weight = randomWeight()) }
        return genome.copy(connectionGenes = updatedConnectionGenes)
    }
}
