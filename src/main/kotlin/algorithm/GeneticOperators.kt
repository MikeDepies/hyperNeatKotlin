package algorithm

import algorithm.crossover.CrossMutation
import algorithm.operator.*

data class GeneticOperators(
    val crossMutation: CrossMutation,
    val mutateAddNode: MutateAddNodeOperator,
    val mutateAddConnection: MutateAddConnectionOperator,
    val mutateWeights: MutateWeightsOperator,
    val mutateActivationFunction: MutateActivationFunctionOperator,
    val mutateConnectionEnabled: GeneticOperator
)

