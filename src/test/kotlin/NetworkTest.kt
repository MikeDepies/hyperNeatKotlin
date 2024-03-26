package algorithm.network

import genome.*
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class NetworkTest : BehaviorSpec({
    given("a simple network genome with one input and one output") {
        val nodeGenomes = listOf(
            NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
            NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)
        )
        val connectionGenes = listOf(
            ConnectionGenome(0, NodeGenome(1, NodeType.INPUT, ActivationFunction.IDENTITY, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 1.0, true)
        )
        val networkGenome = NetworkGenome(nodeGenomes, connectionGenes)

        `when`("the network is built from the genome") {
            val networkBuilder = NetworkBuilder(DefaultActivationFunctionMapper())
            val network = networkBuilder.buildNetworkFromGenome(networkGenome)

            then("it should have 2 nodes and 1 connection") {
                network.nodes.size shouldBe 2
                network.connections.size shouldBe 1
            }
        }
    }

    given("a network genome with multiple connections") {
        val nodeGenomes = listOf(
            NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
            NodeGenome(2, NodeType.HIDDEN, ActivationFunction.RELU, 0.0),
            NodeGenome(3, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)
        )
        val connectionGenes = listOf(
            ConnectionGenome(0, NodeGenome(1, NodeType.INPUT, ActivationFunction.IDENTITY, 0.0), NodeGenome(2, NodeType.HIDDEN, ActivationFunction.RELU, 0.0), 0.5, true),
            ConnectionGenome(1, NodeGenome(2, NodeType.HIDDEN, ActivationFunction.RELU, 0.0), NodeGenome(3, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 1.5, true)
        )
        val networkGenome = NetworkGenome(nodeGenomes, connectionGenes)
        val networkBuilder = NetworkBuilder(DefaultActivationFunctionMapper())
        `when`("the network is built from the genome") {
            val network = networkBuilder.buildNetworkFromGenome(networkGenome)

            then("it should have 3 nodes and 2 connections") {
                network.nodes.size shouldBe 3
                network.connections.size shouldBe 2
            }
        }
    }

    given("a complex network") {
        val nodes = listOf(
            Node(1, NodeType.INPUT, { x: Double -> x }, 0.0),
            Node(2, NodeType.HIDDEN, { x: Double -> Math.max(0.0, x) }, 0.0), // ReLU activation function
            Node(3, NodeType.OUTPUT, { x: Double -> 1 / (1 + Math.exp(-x)) }, 0.0) // Sigmoid activation function
        )
        val connections = listOf(
            Connection(1, 2, 0.5),
            Connection(2, 3, 1.5)
        )
        val network = Network(nodes, connections)

        `when`("input is fed forward through the network") {
            val networkProcessor = NetworkProcessor(network)
            val outputValues = networkProcessor.feedforward(listOf(1.0))

            then("the output should be processed through multiple layers") {
                outputValues.size shouldBe 1
                outputValues[0] shouldBe (1 / (1 + Math.exp(-0.75))) // Expected output after processing through ReLU and then Sigmoid
            }
        }
    }

    // Additional test stubs
    given("an empty network genome") {
        val emptyNodeGenomes = listOf<NodeGenome>()
        val emptyConnectionGenes = listOf<ConnectionGenome>()
        val emptyNetworkGenome = NetworkGenome(emptyNodeGenomes, emptyConnectionGenes)
        val networkBuilder = NetworkBuilder(DefaultActivationFunctionMapper())
        `when`("the network is built from the genome") {
            val network = networkBuilder.buildNetworkFromGenome(emptyNetworkGenome)
            then("it should have 0 nodes and 0 connections") {
                network.nodes.size shouldBe 0
                network.connections.size shouldBe 0
            }
        }
    }
    given("a network genome with disabled connections") {
        val nodeGenomes = listOf(
            NodeGenome(1, NodeType.INPUT, ActivationFunction.IDENTITY, 0.0),
            NodeGenome(2, NodeType.HIDDEN, ActivationFunction.RELU, 0.0),
            NodeGenome(3, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)
        )
        val connectionGenes = listOf(
            ConnectionGenome(0, nodeGenomes[0], nodeGenomes[1], 0.5, enabled = true),
            ConnectionGenome(1, nodeGenomes[1], nodeGenomes[2], 1.5, enabled = false) // This connection is disabled
        )
        val networkGenome = NetworkGenome(nodeGenomes, connectionGenes)
        val networkBuilder = NetworkBuilder(DefaultActivationFunctionMapper())
        `when`("the network is built from the genome") {
            val network = networkBuilder.buildNetworkFromGenome(networkGenome)
            then("it should only include enabled connections") {
                network.connections.size shouldBe 1
                network.connections[0].inputNodeId shouldBe 1
                network.connections[0].outputNodeId shouldBe 2
            }
        }
    }

    given("a network with multiple input and output nodes") {
        `when`("input is fed forward through the network") {
            then("the outputs should be correctly computed from multiple inputs") {
                // Test logic for a network with multiple inputs and outputs
            }
        }
    }
})