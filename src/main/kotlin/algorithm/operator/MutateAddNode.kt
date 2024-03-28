package algorithm.operator

import algorithm.activation.ActivationFunctionSelection
import algorithm.InnovationTracker
import genome.ConnectionGenome
import genome.NetworkGenome
import genome.NodeGenome
import genome.NodeType
import kotlin.random.Random
interface MutateAddNodeOperator : GeneticOperator {
    val random: Random
    val innovationTracker: InnovationTracker
    val connectionInnovationTracker: InnovationTracker
    val activationFunctionSelection: ActivationFunctionSelection
}
class MutateAddNodeOperatorImpl(
    override val random: Random,
    override val innovationTracker: InnovationTracker,
    override val connectionInnovationTracker: InnovationTracker,
    override val activationFunctionSelection: ActivationFunctionSelection
) : MutateAddNodeOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        val randomConnectionIndex = random.nextInt(genome.connectionGenes.size)
        val connectionToSplit = genome.connectionGenes[randomConnectionIndex]

        // Disable the old connection
        val disabledConnection = connectionToSplit.copy(enabled = false)

        // Create a new node
        val newNodeId = innovationTracker.getNextInnovationNumber()
        val newNode = NodeGenome(newNodeId, NodeType.HIDDEN, activationFunctionSelection.select(), 0.0)

        // Create two new connections
        val newConnection1 =
            ConnectionGenome(
                id = connectionInnovationTracker.getNextInnovationNumber(),
                inputNode = connectionToSplit.inputNode,
                outputNode = newNode,
                weight = 1.0,
                enabled = true
            )
        val newConnection2 =
            ConnectionGenome(
                id = connectionInnovationTracker.getNextInnovationNumber(),
                inputNode = newNode,
                outputNode = connectionToSplit.outputNode,
                weight = connectionToSplit.weight,
                enabled = true
            )

        // Update the genome
        val updatedNodeGenomes = genome.nodeGenomes + newNode
        val updatedConnectionGenes =
                genome.connectionGenes.map {
                    if (it.id == connectionToSplit.id) disabledConnection else it
                } + newConnection1 + newConnection2

        return genome.copy(
                nodeGenomes = updatedNodeGenomes,
                connectionGenes = updatedConnectionGenes
        )
    }
}