package algorithm.operator

import algorithm.InnovationTracker
import genome.ConnectionGenome
import genome.NetworkGenome
import kotlin.random.Random
interface MutateAddConnectionOperator : GeneticOperator {
    val random: Random
    val innovationTracker: InnovationTracker
    val weightRange: ClosedRange<Double>
}
class MutateAddConnectionOperatorImpl(
    override val random: Random,
    override val innovationTracker: InnovationTracker,
    override val weightRange: ClosedRange<Double>
) : MutateAddConnectionOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        val availableNodes = genome.nodeGenomes
        val possibleConnections =
                availableNodes.flatMap { inputNode ->
                    availableNodes.mapNotNull { outputNode ->
                        if (inputNode != outputNode &&
                                        genome.connectionGenes.none {
                                            it.inputNode.id == inputNode.id &&
                                                    it.outputNode.id == outputNode.id
                                        }
                        ) {
                            Pair(inputNode, outputNode)
                        } else null
                    }
                }

        if (possibleConnections.isNotEmpty()) {
            val (inputNode, outputNode) = possibleConnections.random()
            val newConnectionId = innovationTracker.getNextInnovationNumber()
            val newConnection =
                ConnectionGenome(
                    id = newConnectionId,
                    inputNode = inputNode,
                    outputNode = outputNode,
                    weight = randomWeight(),
                    enabled = true
                )

            return genome.copy(connectionGenes = genome.connectionGenes + newConnection)
        }

        // If no possible connections, return the genome as is
        return genome
    }

    fun randomWeight(): Double {
        return (random.nextDouble() * (weightRange.endInclusive - weightRange.start) +
                weightRange.start)
    }
}