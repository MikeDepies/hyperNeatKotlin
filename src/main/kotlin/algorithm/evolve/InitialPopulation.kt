package algorithm.evolve

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
    private val randomWeight: RandomWeight
) : InitialPopulationGenerator {

    override fun generatePopulation(size: Int): List<NetworkGenome> =
        List(size) { generateSingleGenome() }

    override fun generateSingleGenome(): NetworkGenome {
        val totalNodeCount = inputNodeCount + outputNodeCount + hiddenNodeCount
        val nodes = (1..totalNodeCount).map { id ->
            NodeGenome(
                id = id,
                type = when {
                    id <= inputNodeCount -> NodeType.INPUT
                    id <= inputNodeCount + hiddenNodeCount -> NodeType.HIDDEN
                    else -> NodeType.OUTPUT
                },
                activationFunction = activationFunctions.random(),
                bias = randomWeight() // Biases are now initialized to random values within a specified range
            )
        }

        val possibleConnections = nodes.flatMap { outputNode ->
            nodes.mapNotNull { inputNode ->
                if (inputNode.id < outputNode.id && inputNode.type != NodeType.OUTPUT && outputNode.type != NodeType.INPUT) Pair(inputNode, outputNode) else null
            }
        }

        val connections = mutableListOf<ConnectionGenome>()
        if (connectionDensity == 1.0) {
            possibleConnections.forEach { (inputNode, outputNode) ->
                connections.add(ConnectionGenome(
                    id = connections.size + 1,
                    inputNode = inputNode,
                    outputNode = outputNode,
                    weight = randomWeight(),
                    enabled = true
                ))
            }
        } else {
            val desiredConnectionCount = (possibleConnections.size * connectionDensity).toInt()
            while (connections.size < desiredConnectionCount) {
                val (inputNode, outputNode) = possibleConnections.random(random)
                connections.add(ConnectionGenome(
                    id = connections.size + 1,
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
