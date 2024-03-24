import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import algorithm.GeneticOperatorsImpl
import genome.*
import io.kotest.matchers.doubles.shouldBeBetween

class GeneticOperatorTest : BehaviorSpec({
    val random = Random(1234) // Fixed seed for reproducibility
    val weightRange = 0.0..1.0
    val geneticOperators = GeneticOperatorsImpl(random, weightRange)

    given("a genetic operator") {
        `when`("crossover is performed") {
            then("it should produce a child genome with the correct number of node genomes") {
                val parent1 = NetworkGenome(
                    listOf(NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0)),
                    listOf(ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true))
                )
                val parent2 = NetworkGenome(
                    listOf(NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)),
                    listOf(ConnectionGenome(2, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.8, true))
                )
                val child = geneticOperators.crossover(parent1, parent2)
                child.nodeGenomes.size shouldBe 2
            }
            then("it should produce a child genome with the correct number of connection genes") {
                val parent1 = NetworkGenome(
                    listOf(NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0)),
                    listOf(ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true))
                )
                val parent2 = NetworkGenome(
                    listOf(NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)),
                    listOf(ConnectionGenome(2, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.8, true))
                )
                val child = geneticOperators.crossover(parent1, parent2)
                child.connectionGenes.size shouldBe 2
            }
        }

        `when`("mutateAddNode is performed") {
            then("it should increase the number of node genomes by one") {
                val genome = NetworkGenome(
                    listOf(NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)),
                    listOf(ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true))
                )
                val mutatedGenome = geneticOperators.mutateAddNode(genome)
                mutatedGenome.nodeGenomes.size shouldBe 3 // One new node added
            }
            then("it should adjust the number of connection genes correctly") {
                val genome = NetworkGenome(
                    listOf(NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0)),
                    listOf(ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true))
                )
                val mutatedGenome = geneticOperators.mutateAddNode(genome)
                mutatedGenome.connectionGenes.size shouldBe 3 // Two new connections, one disabled
            }
        }

        `when`("mutateAddConnection is performed") {
            then("it should increase the number of connection genes by one") {
                val genome = NetworkGenome(
                    listOf(
                        NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0),
                        NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)
                    ),
                    emptyList()
                )
                val mutatedGenome = geneticOperators.mutateAddConnection(genome)
                mutatedGenome.connectionGenes.size shouldBe 1 // One new connection added
            }
        }

        `when`("mutateWeights is performed") {
            then("it should mutate weights within the specified range") {
                val genome = NetworkGenome(
                    emptyList(),
                    listOf(ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true))
                )
                val epsilon = 0.0001
                val mutatedGenome = geneticOperators.mutateWeights(genome)
                mutatedGenome.connectionGenes.forEach { it.weight.shouldBeBetween(weightRange.start, weightRange.endInclusive, epsilon)}
            }
        }
    }
})
