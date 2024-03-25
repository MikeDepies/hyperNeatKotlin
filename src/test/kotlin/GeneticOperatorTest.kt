//import io.kotest.core.spec.style.BehaviorSpec
//import io.kotest.matchers.shouldBe
//import kotlin.random.Random
//import algorithm.GeneticOperatorsImpl
//import genome.*
//import algorithm.InnovationTracker
//import io.kotest.matchers.doubles.shouldBeBetween
//
//class GeneticOperatorTest : BehaviorSpec({
//    val random = Random(1234) // Fixed seed for reproducibility
//    val weightRange = 0.0..1.0
//    val activationFunctions = ActivationFunction.values().toList()
//    val innovationTracker = InnovationTracker()
//    val nodeInnovationTracker = InnovationTracker()
//    val geneticOperators = GeneticOperatorsImpl(random, weightRange, activationFunctions, innovationTracker, nodeInnovationTracker)
//
//    given("a genetic operator") {
//        `when`("crossover is performed") {
//            then("it should produce a child genome with the correct number of node genomes for a small network") {
//                val parent1 = NetworkGenome(
//                    listOf(NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0)),
//                    listOf(ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true))
//                )
//                val parent2 = NetworkGenome(
//                    listOf(NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)),
//                    listOf(ConnectionGenome(2, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.8, true))
//                )
//                val child = geneticOperators.crossover(parent1, parent2)
//                child.nodeGenomes.size shouldBe 2
//            }
//            then("it should produce a child genome with the correct number of connection genes for a small network") {
//                val parent1 = NetworkGenome(
//                    listOf(NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0)),
//                    listOf(ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true))
//                )
//                val parent2 = NetworkGenome(
//                    listOf(NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)),
//                    listOf(ConnectionGenome(2, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.8, true))
//                )
//                val child = geneticOperators.crossover(parent1, parent2)
//                child.connectionGenes.size shouldBe 2
//            }
//            then("it should work correctly for a medium-sized network") {
//                val parent1 = NetworkGenome(
//                    (1..5).map { NodeGenome(it, NodeType.HIDDEN, ActivationFunction.RELU, 0.0) },
//                    (1..5).map { ConnectionGenome(it, NodeGenome(it, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome((it+1)%5 + 1, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true) }
//                )
//                val parent2 = NetworkGenome(
//                    (6..10).map { NodeGenome(it, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.0) },
//                    (6..10).map { ConnectionGenome(it, NodeGenome(it, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0), NodeGenome((it+1)%5 + 6, NodeType.OUTPUT, ActivationFunction.RELU, 0.0), 0.8, true) }
//                )
//                val child = geneticOperators.crossover(parent1, parent2)
//                child.nodeGenomes.size shouldBe 10
//                child.connectionGenes.size shouldBe 10
//            }
//            then("it should work correctly for a large-sized network") {
//                val parent1 = NetworkGenome(
//                    (1..50).map { NodeGenome(it, NodeType.HIDDEN, ActivationFunction.RELU, 0.0) },
//                    (1..50).map { ConnectionGenome(it, NodeGenome(it, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome((it+1)%50 + 1, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true) }
//                )
//                val parent2 = NetworkGenome(
//                    (51..100).map { NodeGenome(it, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.0) },
//                    (51..100).map { ConnectionGenome(it, NodeGenome(it, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0), NodeGenome((it+1)%50 + 51, NodeType.OUTPUT, ActivationFunction.RELU, 0.0), 0.8, true) }
//                )
//                val child = geneticOperators.crossover(parent1, parent2)
//                child.nodeGenomes.size shouldBe 100
//                child.connectionGenes.size shouldBe 100
//            }
//            then("it should work correctly for a network with 3 hidden nodes, 2 input nodes, and 1 output node, fully connected") {
//                val parent1 = NetworkGenome(
//                    listOf(
//                        NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0),
//                        NodeGenome(2, NodeType.INPUT, ActivationFunction.RELU, 0.0),
//                        NodeGenome(3, NodeType.HIDDEN, ActivationFunction.RELU, 0.0),
//                        NodeGenome(4, NodeType.HIDDEN, ActivationFunction.RELU, 0.0),
//                        NodeGenome(5, NodeType.HIDDEN, ActivationFunction.RELU, 0.0),
//                        NodeGenome(6, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)
//                    ),
//                    (1..6).flatMap { input ->
//                        (1..6).mapNotNull { output ->
//                            if (input != output && input < output) ConnectionGenome(input * 10 + output, NodeGenome(input, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(output, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true)
//                            else null
//                        }
//                    }
//                )
//                val parent2 = NetworkGenome(
//                    listOf(
//                        NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
//                        NodeGenome(2, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
//                        NodeGenome(3, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.0),
//                        NodeGenome(4, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.0),
//                        NodeGenome(5, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.0),
//                        NodeGenome(6, NodeType.OUTPUT, ActivationFunction.RELU, 0.0)
//                    ),
//                    (1..6).flatMap { input ->
//                        (1..6).mapNotNull { output ->
//                            if (input != output && input < output) ConnectionGenome(input * 10 + output, NodeGenome(input, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0), NodeGenome(output, NodeType.OUTPUT, ActivationFunction.RELU, 0.0), 0.8, true)
//                            else null
//                        }
//                    }
//                )
//                val child = geneticOperators.crossover(parent1, parent2)
//                child.nodeGenomes.size shouldBe 6
//                child.connectionGenes.size shouldBe ((2 + 3) * 4) // Fully connected network with 2 inputs, 3 hidden, and 1 output node
//            }
//        }
//
//        `when`("mutateAddNode is performed") {
//            then("it should increase the number of node genomes by one") {
//                val genome = NetworkGenome(
//                    listOf(NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)),
//                    listOf(ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true))
//                )
//                val mutatedGenome = geneticOperators.mutateAddNode(genome)
//                mutatedGenome.nodeGenomes.size shouldBe 3 // One new node added
//            }
//            then("it should adjust the number of connection genes correctly") {
//                val genome = NetworkGenome(
//                    listOf(NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0)),
//                    listOf(ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true))
//                )
//                val mutatedGenome = geneticOperators.mutateAddNode(genome)
//                mutatedGenome.connectionGenes.size shouldBe 3 // Two new connections, one disabled
//            }
//        }
//
//        `when`("mutateAddConnection is performed") {
//            then("it should increase the number of connection genes by one") {
//                val genome = NetworkGenome(
//                    listOf(
//                        NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0),
//                        NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0)
//                    ),
//                    emptyList()
//                )
//                val mutatedGenome = geneticOperators.mutateAddConnection(genome)
//                mutatedGenome.connectionGenes.size shouldBe 1 // One new connection added
//            }
//        }
//
//        `when`("mutateWeights is performed") {
//            then("it should mutate weights within the specified range") {
//                val genome = NetworkGenome(
//                    emptyList(),
//                    listOf(ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 0.0), 0.5, true))
//                )
//                val epsilon = 0.0001
//                val mutatedGenome = geneticOperators.mutateWeights(genome)
//                mutatedGenome.connectionGenes.forEach { it.weight.shouldBeBetween(weightRange.start, weightRange.endInclusive, epsilon)}
//            }
//        }
//    }
//})
