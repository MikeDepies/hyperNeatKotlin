package algorithm.operator

import algorithm.InnovationTracker
import algorithm.weight.RandomWeight
import genome.ConnectionGenome
import genome.NetworkGenome
import kotlin.random.Random

interface MutateAddConnectionOperator : GeneticOperator {
    val random: Random
    val innovationTracker: InnovationTracker
    val randomWeight: RandomWeight // Keep only the RandomWeight
}

class MutateAddConnectionOperatorImpl(
    override val random: Random,
    override val innovationTracker: InnovationTracker,
    override val randomWeight: RandomWeight // Remove weightRange and use RandomWeight
) : MutateAddConnectionOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        val existingConnections = genome.connectionGenes.map { it.inputNode.id to it.outputNode.id }.toSet()

        val possibleConnections = genome.nodeGenomes.flatMap { inputNode ->
            genome.nodeGenomes.mapNotNull { outputNode ->
                if (inputNode != outputNode && (inputNode.id to outputNode.id) !in existingConnections) {
                    Pair(inputNode, outputNode)
                } else null
            }
        }

        if (possibleConnections.isNotEmpty()) {
            val (inputNode, outputNode) = possibleConnections.random(random)
            val newConnectionId = innovationTracker.getNextInnovationNumber()
            val newConnection = ConnectionGenome(
                id = newConnectionId,
                inputNode = inputNode,
                outputNode = outputNode,
                weight = randomWeight.invoke(), // Use the RandomWeight instance directly
                enabled = true
            )

            return genome.copy(connectionGenes = genome.connectionGenes + newConnection)
        }

        return genome
    }
}
