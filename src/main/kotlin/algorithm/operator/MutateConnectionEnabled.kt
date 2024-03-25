package algorithm.operator

import genome.NetworkGenome
import kotlin.random.Random
interface MutateConnectionEnabledOperator : GeneticOperator {
    val random: Random
}
class MutateConnectionEnabledOperatorImpl(override val random: Random) : MutateConnectionEnabledOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        val updatedConnectionGenes = genome.connectionGenes.map { it.copy(enabled = !it.enabled) }
        return genome.copy(connectionGenes = updatedConnectionGenes)
    }
}