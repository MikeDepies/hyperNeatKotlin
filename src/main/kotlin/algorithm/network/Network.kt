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
        ActivationFunction.GAUSSIAN -> { x -> Math.exp(-Math.pow(x, 2.0)) }
        ActivationFunction.SINE -> { x -> Math.sin(x) }
        ActivationFunction.COS -> { x -> Math.cos(x) }
        ActivationFunction.ABS -> { x -> Math.abs(x) }
        ActivationFunction.STEP -> { x -> if (x < 0) 0.0 else 1.0 }
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

interface NetworkProcessor {
    fun feedforward(inputValues: List<Double>): List<Double>
}

class NetworkProcessorSimple(private val network: Network) : NetworkProcessor {
    val outputNodes = network.nodes.filter { it.type == NodeType.OUTPUT }

    override fun feedforward(inputValues: List<Double>): List<Double> {
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
class NetworkProcessorStateful(private val network: Network, val maxIterations: Int = 10, val convergenceThreshold: Double = 0.01) : NetworkProcessor {
    private val outputNodes = network.nodes.filter { it.type == NodeType.OUTPUT }

    override fun feedforward(inputValues: List<Double>): List<Double> {
        resetInputValues()
        assignInputValues(inputValues)

        var previousOutputValues = outputNodes.map(Node::outputValue)
        var iteration = 0
        var converged = false
        while (iteration < maxIterations && !converged) {
            processNodes()

            val currentOutputValues = outputNodes.map(Node::outputValue)
            converged = isConverged(previousOutputValues, currentOutputValues, convergenceThreshold)
            if (!converged) {
                previousOutputValues = currentOutputValues
            }
            iteration++
        }

        return outputNodes.map(Node::outputValue)
    }

    private fun resetInputValues() = network.nodes.forEach { it.inputValue = 0.0 }

    private fun assignInputValues(inputValues: List<Double>) {
        network.nodes.filter { it.type == NodeType.INPUT }
            .take(inputValues.size)
            .forEachIndexed { index, node -> node.inputValue = inputValues[index] }
    }

    private fun processNodes() {
        val inputConnectionsByOutputNodeId = network.connections.groupBy(Connection::outputNodeId)
        network.nodes.sortedBy { it.type.ordinal }.forEach { node ->
            inputConnectionsByOutputNodeId[node.id]?.forEach { connection ->
                network.nodes.find { it.id == connection.inputNodeId }?.apply {
                    node.inputValue += this.outputValue * connection.weight
                }
            }
            node.activate()
        }
    }
    private fun isConverged(previousOutputValues: List<Double>, currentOutputValues: List<Double>, threshold: Double): Boolean {
        for (i in previousOutputValues.indices) {
            if (kotlin.math.abs(previousOutputValues[i] - currentOutputValues[i]) >= threshold) return false
        }
        return true
    }
}

class NetworkProcessorFactory(val networkBuilder: NetworkBuilder) {
    fun createProcessor(genome: NetworkGenome): NetworkProcessor {
        return if (NetworkCycleTester(networkBuilder.buildNetworkFromGenome(genome)).hasCyclicConnections()) {
            NetworkProcessorStateful(networkBuilder.buildNetworkFromGenome(genome), maxIterations = 10, convergenceThreshold = 0.01)
        } else {
            NetworkProcessorSimple(networkBuilder.buildNetworkFromGenome(genome))
        }
    }
}

class NetworkCycleTester(val network: Network) {
    fun hasCyclicConnections(): Boolean {
        val visited = hashSetOf<Int>()
        val recStack = hashSetOf<Int>()

        for (node in network.nodes) {
            if (dfs(node.id, visited, recStack)) return true
        }
        return false
    }

    private fun dfs(currentNodeId: Int, visited: HashSet<Int>, recStack: HashSet<Int>): Boolean {
        if (recStack.contains(currentNodeId)) return true
        if (visited.contains(currentNodeId)) return false

        visited.add(currentNodeId)
        recStack.add(currentNodeId)

        val childNodes = network.connections.filter { it.inputNodeId == currentNodeId }.map { it.outputNodeId }
        for (childNodeId in childNodes) {
            if (dfs(childNodeId, visited, recStack)) return true
        }

        recStack.remove(currentNodeId)
        return false
    }

}

class NetworkGenomeTester {

    fun hasCyclicConnections(genome: NetworkGenome): Boolean {
        val visited = hashSetOf<Int>()
        val recStack = hashSetOf<Int>()

        for (nodeGenome in genome.nodeGenomes) {
            if (dfs(nodeGenome.id, genome, visited, recStack)) return true
        }
        return false
    }

    private fun dfs(currentNodeId: Int, genome: NetworkGenome, visited: HashSet<Int>, recStack: HashSet<Int>): Boolean {
        if (recStack.contains(currentNodeId)) return true
        if (visited.contains(currentNodeId)) return false

        visited.add(currentNodeId)
        recStack.add(currentNodeId)

        val childNodeIds = genome.connectionGenes
            .filter { it.inputNode.id == currentNodeId }
            .map { it.outputNode.id }

        for (childNodeId in childNodeIds) {
            if (dfs(childNodeId, genome, visited, recStack)) return true
        }

        recStack.remove(currentNodeId)
        return false
    }
}