package algorithm

import algorithm.activation.ActivationFunctionSelection
import algorithm.crossover.CrossMutation
import algorithm.crossover.RandomCrossover
import algorithm.operator.*
import algorithm.weight.SimpleRandomWeight
import algorithm.weight.GaussianRandomWeight
import algorithm.weight.RandomWeight
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
data class WeightMutationConfig(
    val randomWeight: RandomWeight,
    val perturbationChance: Double,
    val perturbationAmount: ClosedRange<Double>
)
fun createDefaultGeneticOperators(
    weightRange: ClosedRange<Double>,
    activationFunctions: List<ActivationFunction>,
    random: Random,
    nodeInnovationTracker: InnovationTracker,
    connectionInnovationTracker: InnovationTracker,
    activationSelection: ActivationFunctionSelection,
    weightMutationConfig: WeightMutationConfig
): GeneticOperators {
    return GeneticOperators(
        RandomCrossover(random),
        MutateAddNodeOperatorImpl(random, nodeInnovationTracker, connectionInnovationTracker, activationSelection),
        MutateAddConnectionOperatorImpl(random, connectionInnovationTracker, weightRange),
        MutateWeightsOperatorImpl(weightMutationConfig.randomWeight, random, weightMutationConfig.perturbationChance, weightMutationConfig.perturbationAmount),
        MutateRandomActivationFunction(random, activationFunctions),
        MutateConnectionEnabledOperatorImpl(random)
    )
}
