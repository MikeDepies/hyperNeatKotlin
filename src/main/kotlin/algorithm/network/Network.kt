package algorithm.network

import genome.*

class Node(val id: Int, val type: NodeType, var activationFunction: (Double) -> Double, var bias: Double) {
    var inputValue: Double = 0.0
    var outputValue: Double = 0.0

    fun activate() {
        outputValue = activationFunction(inputValue + bias)
    }
}

class Network(val nodes: MutableList<Node> = mutableListOf(), val connections: MutableList<Pair<Int, Int>> = mutableListOf())

fun buildNetworkFromGenome(genome: NetworkGenome): Network {
    val network = Network()
    genome.nodeGenomes.forEach { nodeGenome ->
        val activationFunction = when (nodeGenome.activationFunction) {
            ActivationFunction.SIGMOID -> { x: Double -> 1 / (1 + Math.exp(-x)) }
            // Add other activation functions here
            else -> { x: Double -> x } // Default to identity function
        }
        network.nodes.add(Node(nodeGenome.id, nodeGenome.type, activationFunction, nodeGenome.bias))
    }
    genome.connectionGenes.filter { it.enabled }.forEach { connectionGene ->
        network.connections.add(Pair(connectionGene.inputNode.id, connectionGene.outputNode.id))
    }
    return network
}
fun Network.feedforward(inputValues: List<Double>): List<Double> {
    // Reset node input values
    nodes.forEach { it.inputValue = 0.0 }

    // Assign input values to input nodes
    val inputNodes = nodes.filter { it.type == NodeType.INPUT }
    inputValues.forEachIndexed { index, value ->
        inputNodes[index].inputValue = value
    }

    // Activate nodes in order: INPUT -> HIDDEN -> OUTPUT
    val sortedNodes = nodes.sortedBy { it.type }
    sortedNodes.forEach { node ->
        // Sum inputs from connections
        connections.filter { it.second == node.id }.forEach { connection ->
            val inputNode = nodes.find { it.id == connection.first }!!
            node.inputValue += inputNode.outputValue
        }
        node.activate()
    }

    // Collect and return output values
    return nodes.filter { it.type == NodeType.OUTPUT }.map { it.outputValue }
}