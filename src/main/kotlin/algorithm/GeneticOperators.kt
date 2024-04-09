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
    val randomWeight: RandomWeight,
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
    crossMutation: CrossMutation,
    random: Random,
    nodeInnovationTracker: InnovationTracker,
    connectionInnovationTracker: InnovationTracker,
    activationSelection: ActivationFunctionSelection,
    weightMutationConfig: WeightMutationConfig,
    allowCyclicConnections: Boolean,
    allowSelfConnections: Boolean,
    allowOutputAsSource: Boolean,
    allowInputAsTarget: Boolean,
): GeneticOperators {
    return GeneticOperators(
        weightMutationConfig.randomWeight,
        crossMutation,
        MutateAddNodeOperatorImpl(random, nodeInnovationTracker, connectionInnovationTracker, activationSelection, weightMutationConfig.randomWeight),
        MutateAddConnectionOperatorImpl(random, connectionInnovationTracker, weightMutationConfig.randomWeight, allowOutputAsSource, allowInputAsTarget, allowCyclicConnections, allowSelfConnections, ),
        MutateWeightsOperatorImpl(weightMutationConfig.randomWeight, random, weightMutationConfig.perturbationChance, weightMutationConfig.perturbationAmount),
        MutateEveryActivationFunction(random, activationSelection),
        MutateConnectionEnabledOperatorImpl(random)
    )
}
