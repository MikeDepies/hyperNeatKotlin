package algorithm

import algorithm.activation.ActivationFunctionSelection
import algorithm.crossover.CrossMutation
import algorithm.crossover.RandomCrossover
import algorithm.operator.*
import algorithm.weight.SimpleRandomWeight
import genome.ActivationFunction
import kotlin.random.Random

data class GeneticOperators(
    val crossMutation: CrossMutation,
    val mutateAddNode: MutateAddNodeOperator,
    val mutateAddConnection: MutateAddConnectionOperator,
    val mutateWeights: MutateWeightsOperator,
    val mutateActivationFunction: MutateActivationFunctionOperator,
    val mutateConnectionEnabled: GeneticOperator
)

fun createDefaultGeneticOperators(
    weightRange: ClosedRange<Double>,
    activationFunctions: List<ActivationFunction>,
    random: Random,
    nodeInnovationTracker: InnovationTracker,
    connectionInnovationTracker: InnovationTracker,
    activationSelection: ActivationFunctionSelection
): GeneticOperators {
    return GeneticOperators(
        RandomCrossover(random),
        MutateAddNodeOperatorImpl(random, nodeInnovationTracker, activationSelection),
        MutateAddConnectionOperatorImpl(random, connectionInnovationTracker, weightRange),
        MutateWeightsOperatorImpl(SimpleRandomWeight(random, weightRange)),
        MutateRandomActivationFunction(random, activationFunctions),
        MutateConnectionEnabledOperatorImpl(random)
    )
}
