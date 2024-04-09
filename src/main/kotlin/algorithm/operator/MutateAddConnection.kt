package algorithm.operator

import algorithm.InnovationTracker
import algorithm.network.NetworkCycleTester
import algorithm.network.NetworkGenomeTester
import algorithm.weight.RandomWeight
import genome.ConnectionGenome
import genome.NetworkGenome
import genome.NodeGenome
import genome.NodeType
import kotlin.random.Random

interface MutateAddConnectionOperator : GeneticOperator {
    val random: Random
    val innovationTracker: InnovationTracker
    val randomWeight: RandomWeight
}
class MutateAddConnectionOperatorImpl(
    override val random: Random,
    override val innovationTracker: InnovationTracker,
    override val randomWeight: RandomWeight,
    val allowOutputAsSource: Boolean = false, // Default to false
    val allowInputAsTarget: Boolean = false, // Default to false, new option to toggle input as target
    val allowCyclicConnections: Boolean = false, // Default to false, new option to toggle cyclic connections
    val allowSelfConnections: Boolean = false // Default to false, new option to toggle self connections
) : MutateAddConnectionOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        val existingConnections = genome.connectionGenes.map { it.inputNode.id to it.outputNode.id }.toSet()

        val possibleConnections = genome.nodeGenomes.flatMap { inputNode ->
            genome.nodeGenomes.mapNotNull { outputNode ->
                if ((inputNode != outputNode || (allowSelfConnections && allowCyclicConnections)) && (inputNode.id to outputNode.id) !in existingConnections) {
                    if ((!allowOutputAsSource && inputNode.type == NodeType.OUTPUT) ||
                        (!allowInputAsTarget && outputNode.type == NodeType.INPUT) ||
                        (!allowCyclicConnections && wouldFormCycle(genome, inputNode, outputNode))) null
                    else Pair(inputNode, outputNode)
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
                weight = randomWeight.invoke(),
                enabled = true
            )
            // println("Adding connection $newConnectionId from ${newConnection.inputNode.id} to ${newConnection.outputNode.id}")

            return genome.copy(connectionGenes = genome.connectionGenes + newConnection,
                fitness = null,
                sharedFitness = null,
                speciesId = null)
        }

        return genome
    }

    private fun wouldFormCycle(genome: NetworkGenome, inputNode: NodeGenome, outputNode: NodeGenome): Boolean {
        // Create a temporary list of connections including the new potential connection
        val tempConnections = genome.connectionGenes.toMutableList().apply {
            add(ConnectionGenome(
                id = -1, // Temporary ID, as it's not yet part of the genome
                inputNode = inputNode,
                outputNode = outputNode,
                weight = 0.0, // Placeholder weight
                enabled = true
            ))
        }

        // Convert the NetworkGenome to a Network structure expected by NetworkCycleTester
        val tempNetwork = NetworkGenome(
            nodeGenomes = genome.nodeGenomes,
            connectionGenes = tempConnections
        )

        // Use NetworkCycleTester to check for cycles
        val cycleTester = NetworkGenomeTester()
        return cycleTester.hasCyclicConnections(tempNetwork)
    }
}
