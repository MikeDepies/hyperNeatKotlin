package algorithm.operator

import algorithm.activation.ActivationFunctionSelection
import algorithm.InnovationTracker
import algorithm.weight.RandomWeight
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
    override val activationFunctionSelection: ActivationFunctionSelection,
    val randomWeight: RandomWeight
) : MutateAddNodeOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        val enabledConnections = genome.connectionGenes.filter { it.enabled }
        if (enabledConnections.isEmpty()) return genome
//        val randomConnectionIndex = random.nextInt(enabledConnections.size)
        val connectionToSplit = genome.connectionGenes.random(random)

        // Disable the old connection
        val disabledConnection = connectionToSplit.copy(enabled = false)

        // Create a new node
        val newNodeId = innovationTracker.getNextInnovationNumber()
        val newNode = NodeGenome(newNodeId, NodeType.HIDDEN, activationFunctionSelection.select(), randomWeight())

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
//        println("=====****====")
//        println("$connectionToSplit")
//        println("$newConnection1")
//        println("$newConnection2")
        // Update the genome
        val updatedNodeGenomes = genome.nodeGenomes + newNode
        val updatedConnectionGenes =
            genome.connectionGenes.map {
                if (it.id == connectionToSplit.id)
                    disabledConnection else it
            } + newConnection1 + newConnection2
//        println("<<<<")
//        println(genome.connectionGenes.takeLast(5))
//        println(">>>")
//        println(updatedConnectionGenes.takeLast(5))
//        println("Adding node $newNodeId between ${newConnection1.inputNode.id} and ${newConnection2.outputNode.id}")
        return genome.copy(
            nodeGenomes = updatedNodeGenomes,
            connectionGenes = updatedConnectionGenes,
            fitness = null,
            sharedFitness = null,
            speciesId = null
        )//.also { println("new network:\n$it $\n\n") }
    }
}