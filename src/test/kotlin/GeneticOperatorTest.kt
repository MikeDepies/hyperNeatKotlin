import algorithm.InnovationTracker
import algorithm.activation.SingleActivationFunctionSelection
import algorithm.operator.*
import algorithm.weight.SimpleRandomWeight
import genome.ConnectionGenome
import genome.NetworkGenome
import genome.NodeGenome
import genome.NodeType
import genome.ActivationFunction
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random

class GeneticOperatorTest : BehaviorSpec({
    val random = Random
    given("a network genome") {
        val nodeGenomes = listOf(NodeGenome(1, NodeType.INPUT, ActivationFunction.IDENTITY, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.IDENTITY, 0.0))
        val connectionGenomes = listOf(ConnectionGenome(1, nodeGenomes[0], nodeGenomes[1], 0.5, true))
        val networkGenome = NetworkGenome(nodeGenomes, connectionGenomes)

        `when`("mutate add node is applied") {
            val mutateAddNode = MutateAddNodeOperatorImpl(random, InnovationTracker(),  InnovationTracker(), SingleActivationFunctionSelection(ActivationFunction.IDENTITY))
            val mutatedGenome = mutateAddNode.apply(networkGenome)
            then("it should add a new node") {
                mutatedGenome.nodeGenomes.size shouldBe 3
                mutatedGenome.connectionGenes.size shouldBe 3
            }
        }

        `when`("mutate add connection is applied") {
            val mutateAddConnection = MutateAddConnectionOperatorImpl(Random, InnovationTracker(), 0.0..1.0)
            val mutatedGenome = mutateAddConnection.apply(networkGenome)
            then("it should add a new connection") {
                mutatedGenome.connectionGenes.size shouldBe 2
            }
        }

        `when`("mutate weights is applied") {
            val mutateWeights = MutateWeightsOperatorImpl(SimpleRandomWeight(random, (-1.0..1.0)), random, .9)
            val mutatedGenome = mutateWeights.apply(networkGenome)
            then("it should update the weight of connections") {
                mutatedGenome.connectionGenes.forEach { it.weight shouldNotBe 0.5 }
            }
        }

        `when`("mutate activation function is applied") {
            val mutateEveryActivationFunction = MutateEveryActivationFunction(random, listOf(ActivationFunction.SIGMOID))
            val mutatedGenome = mutateEveryActivationFunction.apply(networkGenome)
            then("it should change the activation function of nodes") {
                // Assuming we can check the activation function, which is not shown in the context
                mutatedGenome.nodeGenomes.forEach { it.activationFunction shouldNotBe ActivationFunction.IDENTITY }
            }
        }

        `when`("mutate random activation function is applied") {
            val mutateRandomActivationFunction = MutateRandomActivationFunction(random, listOf(ActivationFunction.SIGMOID))
            val mutatedGenome = mutateRandomActivationFunction.apply(networkGenome)
            then("it should change the activation function of a random node") {
                // Assuming we can check the activation function, which is not shown in the context
                mutatedGenome.nodeGenomes.any { it.activationFunction != ActivationFunction.IDENTITY } shouldBe true
            }
        }   

        `when`("mutate connection enabled is applied") {
            val mutateConnectionEnabled = MutateConnectionEnabledOperatorImpl(random)
            val mutatedGenome = mutateConnectionEnabled.apply(networkGenome)
            then("it should toggle the enabled state of a connection") {
                mutatedGenome.connectionGenes.any { !it.enabled } shouldBe true
            }
        }
    }
})
