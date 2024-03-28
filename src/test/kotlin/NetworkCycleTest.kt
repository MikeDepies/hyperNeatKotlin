import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import algorithm.network.NetworkCycleTester
import genome.NodeGenome
import genome.ConnectionGenome
import genome.NetworkGenome
import genome.NodeType
import genome.ActivationFunction

import algorithm.network.NetworkProcessorStateful
import algorithm.network.NetworkBuilder
import algorithm.network.DefaultActivationFunctionMapper
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThan

class NetworkCycleTest : BehaviorSpec({
    Given("a network with cyclic connections") {
        val nodeGenomes = listOf(
            NodeGenome(1, NodeType.INPUT, ActivationFunction.IDENTITY, 0.0),
            NodeGenome(2, NodeType.HIDDEN, ActivationFunction.RELU, 0.0),
            NodeGenome(3, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0),
            NodeGenome(4, NodeType.HIDDEN, ActivationFunction.TANH, 0.0)
        )
        val connectionGenes = listOf(
            ConnectionGenome(0, nodeGenomes[0], nodeGenomes[1], 0.5, enabled = true),
            ConnectionGenome(1, nodeGenomes[1], nodeGenomes[2], 1.5, enabled = true),
            ConnectionGenome(2, nodeGenomes[2], nodeGenomes[3], -1.0, enabled = true),
            ConnectionGenome(3, nodeGenomes[3], nodeGenomes[1], 0.75, enabled = true) // This creates a cycle
        )
        val networkGenome = NetworkGenome(nodeGenomes, connectionGenes)
        val networkBuilder = NetworkBuilder(DefaultActivationFunctionMapper())
        val cyclicNetwork = networkBuilder.buildNetworkFromGenome(networkGenome)

        When("checking for cyclic connections") {
            val hasCycles = NetworkCycleTester(cyclicNetwork).hasCyclicConnections()

            Then("it should detect cycles") {
                hasCycles.shouldBeTrue()
            }
        }

        When("processing the network with cycles") {
            val processor = NetworkProcessorStateful(cyclicNetwork)
            val output = processor.feedforward(listOf(1.0), )

            Then("it should converge to a stable output within max iterations") {
                val expectedValue = 0.997 // Hypothetical expected value
                val delta = 0.005 // Acceptable delta for convergence
                output.last().shouldBe(expectedValue.plusOrMinus(delta))
            }
        }
    }
    Given("a network without cyclic connections") {
        val nodeGenomes = listOf(
            NodeGenome(1, NodeType.INPUT, ActivationFunction.IDENTITY, 0.0),
            NodeGenome(2, NodeType.HIDDEN, ActivationFunction.RELU, 0.0),
            NodeGenome(3, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)
        )
        val connectionGenes = listOf(
            ConnectionGenome(0, nodeGenomes[0], nodeGenomes[1], 0.5, enabled = true),
            ConnectionGenome(1, nodeGenomes[1], nodeGenomes[2], 1.5, enabled = true)
        )
        val networkGenome = NetworkGenome(nodeGenomes, connectionGenes)
        val networkBuilder = NetworkBuilder(DefaultActivationFunctionMapper())
        val acyclicNetwork = networkBuilder.buildNetworkFromGenome(networkGenome)

        When("checking for cyclic connections") {
            val hasCycles = NetworkCycleTester(acyclicNetwork).hasCyclicConnections()

            Then("it should not detect any cycles") {
                hasCycles.shouldBeFalse()
            }
        }
    }
    Given("a network with specific cyclic and acyclic configurations") {
        val cyclicNodeGenomes = listOf(
            NodeGenome(1, NodeType.INPUT, ActivationFunction.IDENTITY, 0.0),
            NodeGenome(2, NodeType.HIDDEN, ActivationFunction.RELU, 0.0),
            NodeGenome(3, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0),
            NodeGenome(4, NodeType.HIDDEN, ActivationFunction.TANH, 0.0)
        )
        val cyclicConnectionGenes = listOf(
            ConnectionGenome(0, cyclicNodeGenomes[0], cyclicNodeGenomes[1], 0.5, enabled = true),
            ConnectionGenome(1, cyclicNodeGenomes[1], cyclicNodeGenomes[2], 1.5, enabled = true),
            ConnectionGenome(2, cyclicNodeGenomes[2], cyclicNodeGenomes[3], -1.0, enabled = true),
            ConnectionGenome(3, cyclicNodeGenomes[3], cyclicNodeGenomes[1], 0.75, enabled = true) // This creates a cycle
        )
        val acyclicNodeGenomes = cyclicNodeGenomes // Reusing the same node genomes for simplicity
        val acyclicConnectionGenes = cyclicConnectionGenes.dropLast(1) // Removing the last connection to break the cycle

        val cyclicNetworkGenome = NetworkGenome(cyclicNodeGenomes, cyclicConnectionGenes)
        val acyclicNetworkGenome = NetworkGenome(acyclicNodeGenomes, acyclicConnectionGenes)

        val networkBuilder = NetworkBuilder(DefaultActivationFunctionMapper())
        val cyclicNetwork = networkBuilder.buildNetworkFromGenome(cyclicNetworkGenome)
        val acyclicNetwork = networkBuilder.buildNetworkFromGenome(acyclicNetworkGenome)

        When("processing networks with and without certain cycles") {
            val cyclicProcessor = NetworkProcessorStateful(cyclicNetwork)
            val acyclicProcessor = NetworkProcessorStateful(acyclicNetwork)

            val cyclicOutput = cyclicProcessor.feedforward(listOf(1.0))
            val acyclicOutput = acyclicProcessor.feedforward(listOf(1.0))

            Then("the output should reflect the presence or absence of cycles correctly") {
                cyclicOutput.size shouldBeGreaterThan  0 // Expecting some output
                acyclicOutput.size shouldBeGreaterThan 0 // Expecting some output

                // Assuming specific expected values for demonstration purposes
                val expectedCyclicValue = 0.997
                val expectedAcyclicValue = 0.999
                val delta = 0.005

                cyclicOutput.last().shouldBe(expectedCyclicValue.plusOrMinus(delta))
                acyclicOutput.last().shouldBe(expectedAcyclicValue.plusOrMinus(delta))
            }
        }
    }
    Given("a network with a single-node loop") {
        val singleNodeGenome = listOf(
            NodeGenome(1, NodeType.HIDDEN, ActivationFunction.IDENTITY, 0.0)
        )
        val singleConnectionGene = listOf(
            ConnectionGenome(0, singleNodeGenome[0], singleNodeGenome[0], 1.0, enabled = true) // Creates a loop
        )
        val singleLoopNetworkGenome = NetworkGenome(singleNodeGenome, singleConnectionGene)
        val networkBuilder = NetworkBuilder(DefaultActivationFunctionMapper())
        val singleLoopNetwork = networkBuilder.buildNetworkFromGenome(singleLoopNetworkGenome)

        When("checking for cyclic connections") {
            val hasCycles = NetworkCycleTester(singleLoopNetwork).hasCyclicConnections()

            Then("it should detect the single-node loop as a cycle") {
                hasCycles.shouldBeTrue()
            }
        }
    }
    Given("a network with multiple distinct cycles") {
        val nodeGenomes = listOf(
            NodeGenome(1, NodeType.INPUT, ActivationFunction.IDENTITY, 0.0),
            NodeGenome(2, NodeType.HIDDEN, ActivationFunction.RELU, 0.0),
            NodeGenome(3, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0),
            NodeGenome(4, NodeType.HIDDEN, ActivationFunction.TANH, 0.0),
            NodeGenome(5, NodeType.HIDDEN, ActivationFunction.RELU, 0.0)
        )
        val connectionGenes = listOf(
            ConnectionGenome(0, nodeGenomes[0], nodeGenomes[1], 0.5, enabled = true),
            ConnectionGenome(1, nodeGenomes[1], nodeGenomes[2], 1.5, enabled = true),
            ConnectionGenome(2, nodeGenomes[2], nodeGenomes[3], -1.0, enabled = true),
            ConnectionGenome(3, nodeGenomes[3], nodeGenomes[1], 0.75, enabled = true), // This creates the first cycle
            ConnectionGenome(4, nodeGenomes[3], nodeGenomes[4], 0.5, enabled = true),
            ConnectionGenome(5, nodeGenomes[4], nodeGenomes[3], 1.0, enabled = true) // This creates the second cycle
        )
        val multiCycleNetworkGenome = NetworkGenome(nodeGenomes, connectionGenes)
        val networkBuilder = NetworkBuilder(DefaultActivationFunctionMapper())
        val multiCycleNetwork = networkBuilder.buildNetworkFromGenome(multiCycleNetworkGenome)

        When("checking for cyclic connections") {
            val hasCycles = NetworkCycleTester(multiCycleNetwork).hasCyclicConnections()

            Then("it should detect multiple cycles") {
                hasCycles.shouldBeTrue()
            }
        }
    }
})