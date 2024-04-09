package algorithm.operator

import genome.NetworkGenome
import kotlin.random.Random
interface MutateConnectionEnabledOperator : GeneticOperator {
    val random: Random
}
class MutateConnectionEnabledOperatorImpl(override val random: Random) : MutateConnectionEnabledOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        val index = genome.connectionGenes.indices.random(random)
        val updatedConnectionGenes = genome.connectionGenes.mapIndexed { i, it -> if (i == index) it.copy(enabled = !it.enabled) else it }
        return genome.copy(connectionGenes = updatedConnectionGenes,
            fitness = null,
            sharedFitness = null,
            speciesId = null)
    }
}