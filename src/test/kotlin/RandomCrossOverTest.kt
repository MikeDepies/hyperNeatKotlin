package algorithm.crossover

import genome.NetworkGenome
import genome.NodeGenome
import genome.ConnectionGenome
import genome.NodeType
import genome.ActivationFunction
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class RandomCrossoverTest : BehaviorSpec({
    given("a random crossover with a fixed random seed") {
        val random = Random(1234) // Fixed seed for predictable behavior
        val crossover = RandomCrossover(random)

        `when`("crossover is performed with two parent genomes") {
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

            then("the child genome should have a mix of the parents' genes") {
                child.nodeGenomes.size shouldBe 3 // Expecting nodes from both parents
                child.connectionGenes.size shouldBe 1 // Expecting connections from both parents

                // Note: The exact outcome of the crossover is random, so specific assertions
                // about which parent's genes are chosen are not possible here.
            }
        }
    }
})