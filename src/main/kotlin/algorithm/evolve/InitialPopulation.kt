package algorithm.evolve

import algorithm.InnovationTracker
import genome.NetworkGenome
import genome.NodeType
import genome.NodeGenome
import genome.ConnectionGenome
import genome.ActivationFunction
import kotlin.random.Random
import algorithm.weight.RandomWeight

interface InitialPopulationGenerator {
    fun generatePopulation(size: Int): List<NetworkGenome>
    fun generateSingleGenome(): NetworkGenome
}
class SimpleInitialPopulationGenerator(
    private val inputNodeCount: Int,
    private val outputNodeCount: Int,
    private val hiddenNodeCount: Int,
    private val connectionDensity: Double, // Between 0.0 and 1.0
    private val activationFunctions: List<ActivationFunction>,
    private val random: Random,
    private val randomWeight: RandomWeight,
    private val nodeInnovationTracker: InnovationTracker,
    private val connectionInnovationTracker: InnovationTracker
) : InitialPopulationGenerator {

    override fun generatePopulation(size: Int): List<NetworkGenome> {
        val baseGenome = generateSingleGenome()
        return List(size) {
            baseGenome.copy(
                connectionGenes = baseGenome.connectionGenes.map { it.copy(weight = randomWeight()) },
                nodeGenomes = baseGenome.nodeGenomes.map { 
                    it.copy(
                        bias = randomWeight(),
                        activationFunction = if (activationFunctions.size > 1) activationFunctions.random(random) else it.activationFunction
                    ) 
                }
            )
        }
    }

    override fun generateSingleGenome(): NetworkGenome {
        val totalNodeCount = inputNodeCount + outputNodeCount + hiddenNodeCount
        val nodes = mutableListOf<NodeGenome>()
        for (i in 1..totalNodeCount) {
            val type = when {
                i <= inputNodeCount -> NodeType.INPUT
                i <= inputNodeCount + hiddenNodeCount -> NodeType.HIDDEN
                else -> NodeType.OUTPUT
            }
            val nodeId = nodeInnovationTracker.getNextInnovationNumber()
            nodes.add(NodeGenome(
                id = nodeId,
                type = type,
                activationFunction = activationFunctions.random(),
                bias = randomWeight()
            ))
        }

        val possibleConnections = nodes.flatMap { outputNode ->
            nodes.mapNotNull { inputNode ->
                if (inputNode.id < outputNode.id && inputNode.type != NodeType.OUTPUT && outputNode.type != NodeType.INPUT) Pair(inputNode, outputNode) else null
            }
        }

        val connections = mutableListOf<ConnectionGenome>()
        if (connectionDensity == 1.0) {
            possibleConnections.forEach { (inputNode, outputNode) ->
                val connectionId = connectionInnovationTracker.getNextInnovationNumber()
                connections.add(ConnectionGenome(
                    id = connectionId,
                    inputNode = inputNode,
                    outputNode = outputNode,
                    weight = randomWeight(),
                    enabled = true
                ))
            }
        } else {
            val desiredConnectionCount = (possibleConnections.size * connectionDensity).toInt()
            val selectedConnections = possibleConnections.shuffled(random).take(desiredConnectionCount)
            selectedConnections.forEach { (inputNode, outputNode) ->
                val connectionId = connectionInnovationTracker.getNextInnovationNumber()
                connections.add(ConnectionGenome(
                    id = connectionId,
                    inputNode = inputNode,
                    outputNode = outputNode,
                    weight = randomWeight(),
                    enabled = true
                ))
            }
        }

        return NetworkGenome(nodes, connections)
    }
}
