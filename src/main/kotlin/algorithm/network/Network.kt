package algorithm.network

import genome.*

class Node(
        val id: Int,
        val type: NodeType,
        var activationFunction: (Double) -> Double,
        var bias: Double
) {
    var inputValue: Double = 0.0
    var outputValue: Double = 0.0

    fun activate() {
        outputValue = activationFunction(inputValue + bias)
    }
}

data class Connection(val inputNodeId: Int, val outputNodeId: Int, val weight: Double)

class Network(val nodes: List<Node>, val connections: List<Connection>)
class DefaultActivationFunctionMapper : ActivationFunctionMapper {
    override fun map(activationFunction: ActivationFunction): (Double) -> Double = when (activationFunction) {
        ActivationFunction.IDENTITY -> { x -> x }
        ActivationFunction.SIGMOID -> { x -> 1 / (1 + Math.exp(-x)) }
        ActivationFunction.TANH -> { x -> Math.tanh(x) }
        ActivationFunction.RELU -> { x -> Math.max(0.0, x) }
    }
}

interface ActivationFunctionMapper {
    fun map(activationFunction: ActivationFunction): (Double) -> Double
}

class NetworkBuilder(private val activationFunctionMapper: ActivationFunctionMapper) {

    fun buildNetworkFromGenome(genome: NetworkGenome): Network {
        val nodes = genome.nodeGenomes.map { nodeGenome ->
            val activationFunction = activationFunctionMapper.map(nodeGenome.activationFunction)
            Node(nodeGenome.id, nodeGenome.type, activationFunction, nodeGenome.bias)
        }

        val connections = genome.connectionGenes
            .filter { it.enabled }
            .map { Connection(it.inputNode.id, it.outputNode.id, it.weight) }

        return Network(nodes, connections)
    }
}

class NetworkProcessor(private val network: Network) {
    val outputNodes = network.nodes.filter { it.type == NodeType.OUTPUT }

    fun feedforward(inputValues: List<Double>): List<Double> {
        network.nodes.forEach { it.inputValue = 0.0 } // Reset node input values

        // Efficiently assign input values to input nodes
        network.nodes.filter { it.type == NodeType.INPUT }.take(inputValues.size).forEachIndexed {
                index,
                node ->
            node.inputValue = inputValues[index]
        }

        // Preprocess connections to map output node IDs to their input connections
        val inputConnectionsByOutputNodeId = network.connections.groupBy { it.outputNodeId }

        // Process nodes in order of their types
        network.nodes.sortedBy { it.type.ordinal }.forEach { node ->
            inputConnectionsByOutputNodeId[node.id]?.forEach { connection ->
                network.nodes.find { it.id == connection.inputNodeId }?.let { inputNode ->
                    node.inputValue += inputNode.outputValue * connection.weight
                }
            }
            node.activate()
        }

        // Select for output nodes once, and then use that for each output of the feedforward

        return outputNodes.map { it.outputValue }
    }
}

class NetworkProcessorFactory(val networkBuilder: NetworkBuilder) {
    fun createProcessor(genome: NetworkGenome): NetworkProcessor {
        return NetworkProcessor(networkBuilder.buildNetworkFromGenome(genome))
    }
}

