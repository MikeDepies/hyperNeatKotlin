package algorithm.network

import genome.*
import java.util.Stack

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
    val inputNodes = network.nodes.filter { it.type == NodeType.INPUT }
    val sortedNodes = network.nodes.sortedBy { it.type.ordinal }
    // Preprocess connections to map output node IDs to their input connections
    val inputConnectionsByOutputNodeId = network.connections.groupBy { it.outputNodeId }
    val nodeMap = network.nodes.associateBy { it.id }
    override fun feedforward(inputValues: List<Double>): List<Double> {
        network.nodes.forEach { it.inputValue = 0.0 } // Reset node input values
        
        // Efficiently assign input values to input nodes
        inputNodes.take(inputValues.size).forEachIndexed {
                index,
                node ->
            node.inputValue = inputValues[index]
        }

        
        // Process nodes in order of their types
        sortedNodes.forEach { node ->
            inputConnectionsByOutputNodeId[node.id]?.forEach { connection ->
                nodeMap[connection.inputNodeId]?.let { inputNode ->
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
    private val inputNodes = network.nodes.filter { it.type == NodeType.INPUT }
    val sortedNodes = network.nodes.sortedBy { it.type.ordinal }
    val nodeMap = network.nodes.associateBy { it.id }
    val inputConnectionsByOutputNodeId = network.connections.groupBy { it.outputNodeId }
    override fun feedforward(inputValues: List<Double>): List<Double> {
        resetInputValues()
        assignInputValues(inputValues)

        var previousOutputValues = outputNodes.map(Node::outputValue)
        var iteration = 0
        var converged = false
        
        while (iteration < maxIterations && !converged) {
            processNodes(sortedNodes, nodeMap, inputConnectionsByOutputNodeId)

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
        inputNodes.take(inputValues.size).forEachIndexed { index, node -> node.inputValue = inputValues[index] }
    }

    private fun processNodes(sortedNodes: List<Node>, nodeMap: Map<Int, Node>, inputConnectionsByOutputNodeId: Map<Int, List<Connection>>) {
        sortedNodes.forEach { node ->
            inputConnectionsByOutputNodeId[node.id]?.forEach { connection ->
                nodeMap[connection.inputNodeId]?.apply {
                    node.inputValue += this.outputValue * connection.weight
                }
            }
            node.activate()
        }
    }
    private fun isConverged(previousOutputValues: List<Double>, currentOutputValues: List<Double>, threshold: Double): Boolean {
        return previousOutputValues.zip(currentOutputValues).all { kotlin.math.abs(it.first - it.second) < threshold }
    }
}

class NetworkProcessorFactory(val networkBuilder: NetworkBuilder, val cyclic : Boolean, val maxIterations: Int = 10, val convergenceThreshold: Double = 0.01) {
    fun createProcessor(genome: NetworkGenome): NetworkProcessor {
        return if (cyclic && NetworkGenomeTester().hasCyclicConnections(genome)) {
            NetworkProcessorStateful(networkBuilder.buildNetworkFromGenome(genome), maxIterations, convergenceThreshold)
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
        val stack = mutableListOf<Int>()
        stack.add(currentNodeId)

        while (stack.isNotEmpty()) {
            val nodeId = stack.last()

            if (!visited.contains(nodeId)) {
                if (recStack.contains(nodeId)) return true
                visited.add(nodeId)
                recStack.add(nodeId)
                val childNodes = network.connections.filter { it.inputNodeId == nodeId }.map { it.outputNodeId }
                stack.addAll(childNodes)
            } else {
                recStack.remove(nodeId)
                stack.removeAt(stack.size - 1)
            }
        }
        return false
    }

}
class NetworkGenomeTester {

    fun hasCyclicConnections(genome: NetworkGenome): Boolean {
        val visited = hashSetOf<Int>()
        val recStack = hashSetOf<Int>()

        for (currentNodeId in genome.nodeGenomes.map { it.id }) {
            if (!visited.contains(currentNodeId)) {
                if (isCyclicUtil(currentNodeId, genome, visited, recStack)) {
                    return true
                }
            }
        }
        return false
    }

    private fun isCyclicUtil(startNodeId: Int, genome: NetworkGenome, visited: HashSet<Int>, recStack: HashSet<Int>): Boolean {
        val stack = mutableListOf<Pair<Int, Iterator<Int>>>()

        // Add start node to stack
        stack.add(Pair(startNodeId, getChildNodeIds(startNodeId, genome).iterator()))

        while (stack.isNotEmpty()) {
            val (nodeId, iterator) = stack.last()

            if (!visited.contains(nodeId)) {
                visited.add(nodeId)
                recStack.add(nodeId)
            }

            var cycleDetected = false
            while (iterator.hasNext()) {
                val childNodeId = iterator.next()

                if (recStack.contains(childNodeId)) {
                    cycleDetected = true
                    break
                } else if (!visited.contains(childNodeId)) {
                    stack.add(Pair(childNodeId, getChildNodeIds(childNodeId, genome).iterator()))
                    cycleDetected = false
                    break
                }
            }

            if (cycleDetected) {
                return true
            } else if (!iterator.hasNext()) {
                recStack.remove(nodeId)
                stack.removeAt(stack.size - 1)
            }
        }

        return false
    }

    private fun getChildNodeIds(nodeId: Int, genome: NetworkGenome): List<Int> {
        return genome.connectionGenes
            .filter { it.inputNode.id == nodeId && it.enabled }
            .map { it.outputNode.id }
    }
}