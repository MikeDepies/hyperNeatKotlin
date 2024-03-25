package algorithm.crossover

import genome.NetworkGenome
import genome.NodeGenome
import genome.ConnectionGenome
import genome.NodeType
import genome.ActivationFunction
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class BiasedCrossoverTest : BehaviorSpec({
    given("a biased crossover with a fixed random seed") {
        val random = Random(1234) // Fixed seed for predictable behavior

        listOf(0.0, 1.0).forEach { bias ->
            val crossover = BiasedCrossover(random, bias)

            `when`("crossover is performed with two parent genomes and bias is $bias") {
                val parent1Nodes = listOf(
                    NodeGenome(1, NodeType.INPUT, ActivationFunction.IDENTITY, 0.0),
                    NodeGenome(2, NodeType.OUTPUT, ActivationFunction.IDENTITY, 0.0)
                )
                val parent2Nodes = listOf(
                    NodeGenome(1, NodeType.INPUT, ActivationFunction.IDENTITY, 0.0),
                    NodeGenome(2, NodeType.OUTPUT, ActivationFunction.IDENTITY, 0.0),
                    NodeGenome(3, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.0)
                )
                val parent1Connections = listOf(
                    ConnectionGenome(1, parent1Nodes[0], parent1Nodes[1], 1.0, true)
                )
                val parent2Connections = listOf(
                    ConnectionGenome(1, parent2Nodes[0], parent2Nodes[1], 2.0, true)
                )
                val parent1 = NetworkGenome(parent1Nodes, parent1Connections)
                val parent2 = NetworkGenome(parent2Nodes, parent2Connections)

                val child = crossover.crossover(parent1, parent2)

                then("the child genome should have a mix of the parents' genes with a bias of $bias") {
                    child.nodeGenomes.size shouldBe 3 // Expecting nodes from both parents
                    child.connectionGenes.size shouldBe 1 // Expecting connections from both parents

                    if (bias == 1.0) {
                        val commonNodes = parent1Nodes
                        val commonConnections = parent1Connections
                        child.nodeGenomes.shouldContainAll(commonNodes)
                        child.connectionGenes.shouldContainAll(commonConnections)
                        child.nodeGenomes.forEach { node ->
                            if (node in commonNodes) parent1.nodeGenomes.shouldContain(node)
                        }
                        child.connectionGenes.forEach { connection ->
                            if (connection in commonConnections) parent1.connectionGenes.shouldContain(connection)
                        }
                    } else if (bias == 0.0) {
                        val commonNodes = parent2Nodes
                        val commonConnections = parent2Connections
                        child.nodeGenomes.shouldContainAll(commonNodes)
                        child.connectionGenes.shouldContainAll(commonConnections)
                        child.nodeGenomes.forEach { node ->
                            if (node in commonNodes) parent2.nodeGenomes.shouldContain(node)
                        }
                        child.connectionGenes.forEach { connection ->
                            if (connection in commonConnections) parent2.connectionGenes.shouldContain(connection)
                        }
                    }
                }
            }
        }
    }
})
