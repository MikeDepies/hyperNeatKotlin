package algorithm

import algorithm.activation.ActivationFunctionSelection
import algorithm.crossover.CrossMutation
import algorithm.crossover.RandomCrossover
import algorithm.operator.*
import algorithm.weight.SimpleRandomWeight
import algorithm.weight.GaussianRandomWeight
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
        MutateWeightsOperatorImpl(GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive), random,.7, -0.01..0.01),
        MutateRandomActivationFunction(random, activationFunctions),
        MutateConnectionEnabledOperatorImpl(random)
    )
}
