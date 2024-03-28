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
import algorithm.weight.RandomWeight

class MutateAddConnectionOperatorTest : BehaviorSpec({
    // Common setup for tests
    val random = Random(0) // Fixed seed for reproducibility
    val innovationTracker = InnovationTracker()
    val randomWeight = object : RandomWeight {
        override fun invoke(): Double = 0.5 // Fixed weight for simplicity
    }
    val operator = MutateAddConnectionOperatorImpl(random, innovationTracker, randomWeight)

    given("a network genome with no nodes") {
        val genome = NetworkGenome(emptyList(), emptyList())
        `when`("the mutate add connection operator is applied") {
            val mutatedGenome = operator.apply(genome)
            then("no connections should be added") {
                mutatedGenome.connectionGenes shouldBe emptyList()
            }
        }
    }
    given("a network genome with two unconnected nodes") {
        val genome = NetworkGenome(
            listOf(
                NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
                NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0)
            ),
            emptyList()
        )
        `when`("the mutate add connection operator is applied") {
            val mutatedGenome = operator.apply(genome)
            then("one connection should be added") {
                mutatedGenome.connectionGenes.size shouldBe 1
                mutatedGenome.connectionGenes.first().inputNode.id shouldBe 1
                mutatedGenome.connectionGenes.first().outputNode.id shouldBe 2
            }
        }
    }
    given("a network genome with two nodes already connected") {
        val genome = NetworkGenome(
            listOf(
                NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
                NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0)
            ),
            listOf(
                ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0), 0.5, true)
            )
        )
        `when`("the mutate add connection operator is applied") {
            val mutatedGenome = operator.apply(genome)
            then("no new connections should be added") {
                mutatedGenome.connectionGenes.size shouldBe 1 // Assuming the original genome had one connection
            }
        }
    }
    given("a network genome with potential for a new connection") {
        val genome = NetworkGenome(
            listOf(
                NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
                NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0)
            ),
            emptyList()
        )
        `when`("the mutate add connection operator is applied") {
            val mutatedGenome = operator.apply(genome)
            then("the added connection should have the correct weight") {
                mutatedGenome.connectionGenes.first().weight shouldBe 0.5 // Based on the fixed weight from RandomWeight setup
            }
        }
    }
})