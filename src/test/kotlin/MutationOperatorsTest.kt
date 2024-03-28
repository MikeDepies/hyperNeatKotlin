import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import algorithm.operator.MutateAddNodeOperatorImpl
import genome.NetworkGenome
import genome.NodeGenome
import genome.ConnectionGenome
import genome.NodeType
import algorithm.InnovationTracker
import algorithm.activation.ActivationFunctionSelection
import algorithm.weight.SimpleRandomWeight
import genome.ActivationFunction

class MutationOperatorsTest : BehaviorSpec({
    given("A network genome with a single connection") {
        val random = Random(0)
        val innovationTracker = InnovationTracker()
        val connectionInnovationTracker = InnovationTracker()
        val randomWeight = SimpleRandomWeight(random, -1.0..1.0)
        val activationFunctionSelection = object : ActivationFunctionSelection {
            override fun select(): ActivationFunction = ActivationFunction.SIGMOID
        }
        val mutateAddNodeOperator = MutateAddNodeOperatorImpl(
            random,
            innovationTracker,
            connectionInnovationTracker,
            activationFunctionSelection,
            randomWeight
        )

        `when`("Mutate add node is applied") {
            val originalGenome = NetworkGenome(
                listOf(
                    NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
                    NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0)
                ),
                listOf(
                    ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0), 0.5, true)
                )
            )
            val mutatedGenome = mutateAddNodeOperator.apply(originalGenome)

            then("It should add one node and two connections") {
                mutatedGenome.nodeGenomes.size shouldBe 3
                mutatedGenome.connectionGenes.size shouldBe 3
                mutatedGenome.connectionGenes.count { !it.enabled } shouldBe 1 // The original connection should be disabled
            }
        }
    }

    // Additional test cases as per instructions
    given("A network genome with multiple connections") {
        val random = Random(0)
        val innovationTracker = InnovationTracker()
        val connectionInnovationTracker = InnovationTracker()
        val randomWeight = SimpleRandomWeight(random, -1.0..1.0)
        val activationFunctionSelection = object : ActivationFunctionSelection {
            override fun select(): ActivationFunction = ActivationFunction.SIGMOID
        }
        val originalGenome = NetworkGenome(
            listOf(
                NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
                NodeGenome(2, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.5),
                NodeGenome(3, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0)
            ),
            listOf(
                ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0), NodeGenome(2, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.5), 0.5, true),
                ConnectionGenome(2, NodeGenome(2, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.5), NodeGenome(3, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0), 0.8, true)
            )
        )
        val mutateAddNodeOperator = MutateAddNodeOperatorImpl(
            random,
            innovationTracker,
            connectionInnovationTracker,
            activationFunctionSelection,
            randomWeight
        )
        val mutatedGenome = mutateAddNodeOperator.apply(originalGenome)

        then("It should add one node and two connections, disabling one of the original connections") {
            mutatedGenome.nodeGenomes.size shouldBe 4
            mutatedGenome.connectionGenes.size shouldBe 4
            mutatedGenome.connectionGenes.count { !it.enabled } shouldBe 1 // One of the original connections should be disabled
        }
    }

    given("A network genome with no connections") {
        val random = Random(0)
        val innovationTracker = InnovationTracker()
        val connectionInnovationTracker = InnovationTracker()
        val randomWeight = SimpleRandomWeight(random, -1.0..1.0)
        val activationFunctionSelection = object : ActivationFunctionSelection {
            override fun select(): ActivationFunction = ActivationFunction.SIGMOID
        }
        val originalGenome = NetworkGenome(
            listOf(
                NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
                NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0)
            ),
            emptyList()
        )
        val mutateAddNodeOperator = MutateAddNodeOperatorImpl(
            random,
            innovationTracker,
            connectionInnovationTracker,
            activationFunctionSelection,
            randomWeight
        )
        val mutatedGenome = mutateAddNodeOperator.apply(originalGenome)

        then("It should not add any nodes or connections") {
            mutatedGenome.nodeGenomes.size shouldBe 2
            mutatedGenome.connectionGenes shouldBe emptyList()
        }
    }

    given("A network genome with all connections disabled") {
        val random = Random(0)
        val innovationTracker = InnovationTracker()
        val connectionInnovationTracker = InnovationTracker()
        val randomWeight = SimpleRandomWeight(random, -1.0..1.0)
        val activationFunctionSelection = object : ActivationFunctionSelection {
            override fun select(): ActivationFunction = ActivationFunction.SIGMOID
        }
        val originalGenome = NetworkGenome(
            listOf(
                NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
                NodeGenome(2, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.5),
                NodeGenome(3, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0)
            ),
            listOf(
                ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0), NodeGenome(2, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.5), 0.5, false),
                ConnectionGenome(2, NodeGenome(2, NodeType.HIDDEN, ActivationFunction.SIGMOID, 0.5), NodeGenome(3, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0), 0.8, false)
            )
        )
        val mutateAddNodeOperator = MutateAddNodeOperatorImpl(
            random,
            innovationTracker,
            connectionInnovationTracker,
            activationFunctionSelection,
            randomWeight
        )
        val mutatedGenome = mutateAddNodeOperator.apply(originalGenome)

        then("It should not add any nodes or connections due to all connections being disabled") {
            mutatedGenome.nodeGenomes.size shouldBe 3 // No new nodes should be added
            mutatedGenome.connectionGenes.size shouldBe 2 // No new connections should be added
            mutatedGenome.connectionGenes.count { !it.enabled } shouldBe 2 // Both connections remain disabled
        }
    }

    given("A network genome with a specific random seed of 42") {
        val random = Random(42)
        val innovationTracker = InnovationTracker()
        val connectionInnovationTracker = InnovationTracker()
        val randomWeight = SimpleRandomWeight(random, -1.0..1.0)
        val activationFunctionSelection = object : ActivationFunctionSelection {
            override fun select(): ActivationFunction = ActivationFunction.SIGMOID
        }
        val originalGenome = NetworkGenome(
            listOf(
                NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
                NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0)
            ),
            listOf(
                ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0), 0.5, true)
            )
        )
        val mutateAddNodeOperator = MutateAddNodeOperatorImpl(
            random,
            innovationTracker,
            connectionInnovationTracker,
            activationFunctionSelection,
            randomWeight
        )   
        val mutatedGenome = mutateAddNodeOperator.apply(originalGenome)

        then("It should add a node and two connections with seed 42") {
            mutatedGenome.nodeGenomes.size shouldBe 3
            mutatedGenome.connectionGenes.size shouldBe 3
        }
    }

    given("A network genome with edge case IDs") {
        val random = Random(0)
        val innovationTracker = InnovationTracker()
        val connectionInnovationTracker = InnovationTracker()
        val randomWeight = SimpleRandomWeight(random, -1.0..1.0)
        val activationFunctionSelection = object : ActivationFunctionSelection {
            override fun select(): ActivationFunction = ActivationFunction.SIGMOID
        }
        val originalGenome = NetworkGenome(
            listOf(
                NodeGenome(Int.MIN_VALUE, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0),
                NodeGenome(Int.MAX_VALUE, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0)
            ),
            listOf(
                ConnectionGenome(Int.MIN_VALUE, NodeGenome(Int.MIN_VALUE, NodeType.INPUT, ActivationFunction.SIGMOID, 0.0), NodeGenome(Int.MAX_VALUE, NodeType.OUTPUT, ActivationFunction.SIGMOID, 1.0), 0.5, true)
            )
        )
        val mutateAddNodeOperator = MutateAddNodeOperatorImpl(
            random,
            innovationTracker,
            connectionInnovationTracker,
            activationFunctionSelection,
            randomWeight
        )
        val mutatedGenome = mutateAddNodeOperator.apply(originalGenome)

        then("It should correctly handle edge case IDs during mutation") {
            mutatedGenome.nodeGenomes.any { it.id == Int.MIN_VALUE || it.id == Int.MAX_VALUE } shouldBe true
            mutatedGenome.connectionGenes.any { it.id == Int.MIN_VALUE } shouldBe true
        }
    }

    given("A network genome with nodes using different activation functions (RELU and TANH)") {
        val random = Random(42)
        val innovationTracker = InnovationTracker()
        val connectionInnovationTracker = InnovationTracker()
        val randomWeight = SimpleRandomWeight(random, -1.0..1.0)
        // This selection mechanism will always choose RELU for new nodes
        val activationFunctionSelection = object : ActivationFunctionSelection {
            override fun select(): ActivationFunction = ActivationFunction.RELU
        }
        // Original genome has one input node with RELU and one output node with TANH
        val originalGenome = NetworkGenome(
            listOf(
                NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0),
                NodeGenome(2, NodeType.OUTPUT, ActivationFunction.TANH, 1.0)
            ),
            listOf(
                ConnectionGenome(1, NodeGenome(1, NodeType.INPUT, ActivationFunction.RELU, 0.0), NodeGenome(2, NodeType.OUTPUT, ActivationFunction.TANH, 1.0), 0.5, true)
            )
        )
        val mutateAddNodeOperator = MutateAddNodeOperatorImpl(
            random,
            innovationTracker,
            connectionInnovationTracker,
            activationFunctionSelection,
            randomWeight
        )
        // Apply mutation to add a node
        val mutatedGenome = mutateAddNodeOperator.apply(originalGenome)

        then("The mutation should result in a genome with three nodes, maintaining original activation functions and adding a new node with RELU") {
            mutatedGenome.nodeGenomes.size shouldBe 3
            mutatedGenome.connectionGenes.size shouldBe 3
            // Ensure original activation functions are preserved and new node uses RELU
            mutatedGenome.nodeGenomes.count { it.activationFunction == ActivationFunction.RELU } shouldBe 2
            mutatedGenome.nodeGenomes.any { it.activationFunction == ActivationFunction.TANH } shouldBe true
        }
    }
})

