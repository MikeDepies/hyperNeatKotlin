package main.mccMaze

import kotlin.random.Random
import algorithm.GeneticOperators
import algorithm.crossover.CrossMutation
import algorithm.crossover.RandomCrossover
import algorithm.crossover.BiasedCrossover
import algorithm.fromList
import algorithm.MutationOperation
import algorithm.CrossOverOperation
import algorithm.weight.GaussianRandomWeight
import algorithm.InnovationTracker
import algorithm.WeightMutationConfig
import algorithm.evolve.InitialPopulationGenerator
import algorithm.evolve.SimpleInitialPopulationGenerator
import algorithm.DefaultGenomeMutator
import algorithm.GenomeMutator
import algorithm.activation.ActivationFunctionSelection
import algorithm.activation.SingleActivationFunctionSelection
import algorithm.activation.RandomActivationFunctionSelection
import algorithm.createDefaultGeneticOperators
import algorithm.GenomeMutatorOperationConfig
import algorithm.weight.RandomWeight
import coevolution.*
import environment.MazeThresholds
import environment.Position
import environment.TmazeEnvironment
import environment.shortestPathToGoal
import genome.NetworkGenome
import genome.ActivationFunction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.Executors

data class WeightRangesConfig(
    val weightRange: ClosedFloatingPointRange<Double>,
    val perturbationChance: Double,
    val weightPerturbation: ClosedFloatingPointRange<Double>,
    val allowCyclicConnections: Boolean = false,
    val allowSelfConnections: Boolean = false,
    val allowOutputAsSource: Boolean = false,
    val allowInputAsTarget: Boolean = false,
    // val mazeWeightPerturbation: ClosedFloatingPointRange<Double>
)

data class PopulationConfig(
    val size: Int,
    val batchSize: Int,
    val mutationConfig: MutationConfig,
    val nodeInnovationNumber: Int,
    val connectionInnovationNumber: Int,
    val inputNodes: Int,
    val outputNodes: Int,
    val hiddenNodes: Int,
    val connectionDensity: Double,
    val activationFunctions: List<ActivationFunction>
)

fun createPopulationGenerator(
    populationConfig: PopulationConfig,
    nodeInnovationTracker: InnovationTracker,
    connectionInnovationTracker: InnovationTracker,
    random: Random,
    randomWeight: RandomWeight
): InitialPopulationGenerator {
    return SimpleInitialPopulationGenerator(
        populationConfig.inputNodes,
        populationConfig.outputNodes,
        populationConfig.hiddenNodes,
        populationConfig.connectionDensity,
        populationConfig.activationFunctions,
        random,
        randomWeight,
        nodeInnovationTracker,
        connectionInnovationTracker
    )
}

data class SimulationConfig(
    val randomSeed: Int,
    val resourceUsageLimit: Int,
    val stepsAllowed: Int,
    val sensorPositions: List<Position>,
    val mutationOperations: PopulationConfig,
    val mazeMutationOperations: PopulationConfig,
    val threadPoolSize: Int
)

// data class WeightConfig(
//     val mean: Double,
//     val stdDeviation: Double,
//     val mutationRate: Double,
//     val mutationRange: ClosedFloatingPointRange<Double>
// )


fun initializeSimulationConfig(sensorPositions: List<Position>): SimulationConfig {
    val agentActivationFunctions = ActivationFunction.cppn
    val mazeActivationFunctions = ActivationFunction.cppn
    val mutation = MutationConfig(
        WeightRangesConfig(
            weightRange = -3.0..3.0,
            weightPerturbation = -0.001..0.001,
            perturbationChance = 0.9,
            allowCyclicConnections = false, 
            allowSelfConnections = false,
            allowOutputAsSource = false,
            allowInputAsTarget = false
        ),
        listOf(
            MutationOperationConfig(0.02, MutationType.ADD_CONNECTION),
            MutationOperationConfig(0.001, MutationType.ADD_NODE),
            MutationOperationConfig(0.99, MutationType.MUTATE_WEIGHTS),
            MutationOperationConfig(0.02, MutationType.MUTATE_ACTIVATION_FUNCTION),
            MutationOperationConfig(0.02, MutationType.MUTATE_CONNECTION_ENABLED)
        ),
        geneticOperatorConfig = GeneticOperatorConfig(
            ActivationFunctionSelectionMode.Random(agentActivationFunctions),
            CrossMutationMode.Biased(0.7),
            0.7
        )
    )
    val mazeMutation = MutationConfig(
        weightRangesConfig = WeightRangesConfig(
            weightRange = -3.0..3.0,
            weightPerturbation = -0.001..0.001,
            perturbationChance = 0.6,
            allowCyclicConnections = false, 
            allowSelfConnections = false,
            allowOutputAsSource = false,
            allowInputAsTarget = false
        ),
        mutationOperations = listOf(
            MutationOperationConfig(0.03, MutationType.ADD_CONNECTION),
            MutationOperationConfig(0.002, MutationType.ADD_NODE),
            MutationOperationConfig(0.9, MutationType.MUTATE_WEIGHTS),
            MutationOperationConfig(0.02, MutationType.MUTATE_ACTIVATION_FUNCTION),
            MutationOperationConfig(0.02, MutationType.MUTATE_CONNECTION_ENABLED)
        ),
        geneticOperatorConfig = GeneticOperatorConfig(
            ActivationFunctionSelectionMode.Random(mazeActivationFunctions),
            CrossMutationMode.Biased(0.7),
            0.5
        )
    )
    // val sensorPositions = listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1))
    return SimulationConfig(
        randomSeed = 1,
        resourceUsageLimit = 1,
        stepsAllowed = 50,
        threadPoolSize = 16,
        sensorPositions = sensorPositions,
        mutationOperations = PopulationConfig(
            size = 200,
            batchSize = 50,
            mutationConfig = mutation,
            nodeInnovationNumber = 0,
            connectionInnovationNumber = 0,
            inputNodes = 2 + 2 + sensorPositions.size,
            outputNodes = 4,
            hiddenNodes = 0,
            connectionDensity = 1.0,
            activationFunctions = agentActivationFunctions
        ),
        mazeMutationOperations = PopulationConfig(
            size = 50,
            batchSize = 5,
            mutationConfig = mazeMutation,
            nodeInnovationNumber = 0,
            connectionInnovationNumber = 0,
            inputNodes = 3,
            outputNodes = 3,
            hiddenNodes = 2,
            connectionDensity = 1.0,
            activationFunctions = mazeActivationFunctions
        )

    )
}

    //fun initializeWeightMutationConfig(): WeightConfig {
    //    return WeightConfig(
    //        mean = 0.0,
    //        stdDeviation = 1.0,
    //        mutationRate = 0.9,
    //        mutationRange = -0.01..0.01
    //    )
    //}

fun createMutationOperations(geneticOperators: GeneticOperators, random: Random): GenomeMutatorOperationConfig {
    return fromList(
        listOf(
            MutationOperation(0.02, geneticOperators.mutateAddConnection),
            MutationOperation(0.03, geneticOperators.mutateAddNode),
            MutationOperation(0.9, geneticOperators.mutateWeights),
            // MutationOperation(0.01, geneticOperators.mutateActivationFunction),
            MutationOperation(0.02, geneticOperators.mutateConnectionEnabled)
        ), CrossOverOperation(geneticOperators.crossMutation, .1), random
    )
}


fun createMutationOperationsMaze(geneticOperators: GeneticOperators, random: Random): GenomeMutatorOperationConfig {
    return fromList(
        listOf(
            MutationOperation(0.03, geneticOperators.mutateAddConnection),
            MutationOperation(0.03, geneticOperators.mutateAddNode),
            MutationOperation(0.9, geneticOperators.mutateWeights),
            MutationOperation(0.02, geneticOperators.mutateActivationFunction),
            MutationOperation(0.02, geneticOperators.mutateConnectionEnabled)
        ), CrossOverOperation(geneticOperators.crossMutation, .1), random
    )
}

data class SimulationComponents(
    val random: Random,
    val dispatcher: CoroutineDispatcher,

    val weightMutationConfig: GeneticOperators,

    val weightMazeMutationConfig: GeneticOperators,

    val nodeInnovationTracker: InnovationTracker,
    val connectionInnovationTracker: InnovationTracker,
    val mazeNodeInnovationTracker: InnovationTracker,
    val mazeConnectionInnovationTracker: InnovationTracker
)

fun initializeSimulationComponents(simulationConfig: SimulationConfig): SimulationComponents {
    val random = Random(simulationConfig.randomSeed)
    val dispatcher = Executors.newFixedThreadPool(simulationConfig.threadPoolSize).asCoroutineDispatcher()
    val mazeNodeInnovationTracker = InnovationTracker("MazeNode")
    val mazeConnectionInnovationTracker = InnovationTracker("MazeConnection")
    val mazeGenomeMutatorFactory = GeneticOperatorFactory(
        random,
        mazeNodeInnovationTracker,
        mazeConnectionInnovationTracker,
        simulationConfig.mazeMutationOperations.mutationConfig.geneticOperatorConfig,
        simulationConfig.mazeMutationOperations.mutationConfig
    )
    val nodeInnovationTracker = InnovationTracker("AgentNode")
    val connectionInnovationTracker = InnovationTracker("ConnectionNode")
    val geneticOperatorFactory = GeneticOperatorFactory(
        random,
        nodeInnovationTracker,
        connectionInnovationTracker,
        simulationConfig.mutationOperations.mutationConfig.geneticOperatorConfig,
        simulationConfig.mutationOperations.mutationConfig
    )
    val agentGenomeMutator = geneticOperatorFactory.create()
    val mazeGenomeMutator = mazeGenomeMutatorFactory.create()
    return SimulationComponents(
        random = random,
        dispatcher = dispatcher,
        weightMutationConfig = agentGenomeMutator,
        weightMazeMutationConfig = mazeGenomeMutator,
        nodeInnovationTracker = nodeInnovationTracker,
        connectionInnovationTracker = connectionInnovationTracker,
        mazeNodeInnovationTracker = mazeNodeInnovationTracker,
        mazeConnectionInnovationTracker = mazeConnectionInnovationTracker
    )
}

class MccFrameworkFactory(
    private val simulationConfig: SimulationConfig,
    private val simulationComponents: SimulationComponents,
    private val sensorPositions: List<Position>,
    private val agentInitialPopulationGenerator: InitialPopulationGenerator,
    private val mazeInitialPopulationGenerator: InitialPopulationGenerator,
    private val mazeEnvironmentCache: MazeEnvironmentCache,
    private val mazeAgentCache: AgentCache<NetworkGenome, MazeGenome>,
    private val agentGenomeMutator: GenomeMutator,
    private val mazeGenomeMutator: SimpleMazeGenomeMutator,
    private val environmentMCTest: (MCCFramework<NetworkGenome, MazeGenome>, SolvedEnvironment<MazeGenome, NetworkGenome>) -> Boolean,
) {
    var pathReq = 1

    fun create(
        agentQueuePopulation: BatchQueuePopulation<Agent<NetworkGenome, MazeGenome>>,
        environmentQueuePopulation: BatchQueuePopulation<Environment<MazeGenome, NetworkGenome>>
    ): MCCFramework<NetworkGenome, MazeGenome> {
        return MCCFramework(
            simulationComponents.random,
            agentQueuePopulation,
            environmentQueuePopulation,
            simulationComponents.dispatcher,
            maxAgents = agentQueuePopulation.queue.size.toDouble() * .05,
            minAgents = 0.0,
            environmentMCTest = environmentMCTest
        )
    }
}

sealed class ActivationFunctionSelectionMode {
    data class Single(val activationFunction: ActivationFunction) : ActivationFunctionSelectionMode()
    data class Random(val activationFunctions: List<ActivationFunction>) : ActivationFunctionSelectionMode()
}

sealed class CrossMutationMode {
    object Random : CrossMutationMode()
    data class Biased(val bias: Double) : CrossMutationMode()
}

data class GeneticOperatorConfig(
    val activationFunctionSelectionMode: ActivationFunctionSelectionMode,
    val crossMutationMode: CrossMutationMode,
    val crossMutationProbability: Double
)

class ActivationFunctionSelectionFactory(private val random: Random) {
    fun create(activationFunctionSelectionMode: ActivationFunctionSelectionMode): ActivationFunctionSelection {
        return when (activationFunctionSelectionMode) {
            is ActivationFunctionSelectionMode.Single -> SingleActivationFunctionSelection(
                activationFunctionSelectionMode.activationFunction
            )

            is ActivationFunctionSelectionMode.Random -> RandomActivationFunctionSelection(
                random,
                activationFunctionSelectionMode.activationFunctions
            )
        }
    }
}

class CrossMutationFactory(private val random: Random) {
    fun create(crossMutationMode: CrossMutationMode): CrossMutation {
        return when (crossMutationMode) {
            is CrossMutationMode.Random -> RandomCrossover(random)
            is CrossMutationMode.Biased -> BiasedCrossover(random, crossMutationMode.bias)
        }
    }
}

class GeneticOperatorFactory(
    private val random: Random,
    private val nodeInnovationTracker: InnovationTracker,
    private val connectionInnovationTracker: InnovationTracker,
    private val geneticOperatorConfig: GeneticOperatorConfig,
    private val mutationConfig: MutationConfig
) {
    fun create(): GeneticOperators {
        val activationSelection =
            ActivationFunctionSelectionFactory(random).create(geneticOperatorConfig.activationFunctionSelectionMode)
        val crossMutation = CrossMutationFactory(random).create(geneticOperatorConfig.crossMutationMode)
        val weight = GaussianRandomWeight(
            random,
            0.0,
            1.0,
            mutationConfig.weightRangesConfig.weightRange.start,
            mutationConfig.weightRangesConfig.weightRange.endInclusive
        )
        val weightMutationConfig = WeightMutationConfig(
            weight,
            mutationConfig.weightRangesConfig.perturbationChance,
            mutationConfig.weightRangesConfig.weightPerturbation
        )
        return createDefaultGeneticOperators(
            crossMutation,
            random,
            nodeInnovationTracker,
            connectionInnovationTracker,
            activationSelection,
            weightMutationConfig,
            mutationConfig.weightRangesConfig.allowCyclicConnections,
            mutationConfig.weightRangesConfig.allowSelfConnections,
            mutationConfig.weightRangesConfig.allowOutputAsSource,
            mutationConfig.weightRangesConfig.allowInputAsTarget
        )
    }
}

// class GenomeMutatorFactory(private val random: Random, private val simulationComponents: SimulationComponents) {
//     fun create(geneticOperators: GeneticOperators): GenomeMutator {
//         return DefaultGenomeMutator(createMutationOperations(geneticOperators, simulationComponents.random))
//     }
// }


data class MutationOperationConfig(
    val rate: Double,
    val type: MutationType
)


enum class MutationType {
    ADD_CONNECTION,
    ADD_NODE,
    MUTATE_WEIGHTS,
    MUTATE_ACTIVATION_FUNCTION,
    MUTATE_CONNECTION_ENABLED
}

data class MutationConfig(
    val weightRangesConfig: WeightRangesConfig,
    val mutationOperations: List<MutationOperationConfig>,
    val geneticOperatorConfig: GeneticOperatorConfig
)

class MutationOperationFactory(private val random: Random, private val mutationConfig: MutationConfig) {
    fun createFromConfig(geneticOperators: GeneticOperators): GenomeMutator {
        val mutationOperations = mutationConfig.mutationOperations.map { config ->
            when (config.type) {
                MutationType.ADD_CONNECTION -> MutationOperation(config.rate, geneticOperators.mutateAddConnection)
                MutationType.ADD_NODE -> MutationOperation(config.rate, geneticOperators.mutateAddNode)
                MutationType.MUTATE_WEIGHTS -> MutationOperation(config.rate, geneticOperators.mutateWeights)
                MutationType.MUTATE_ACTIVATION_FUNCTION -> MutationOperation(
                    config.rate,
                    geneticOperators.mutateActivationFunction
                )

                MutationType.MUTATE_CONNECTION_ENABLED -> MutationOperation(
                    config.rate,
                    geneticOperators.mutateConnectionEnabled
                )
            }
        }
        return DefaultGenomeMutator(
            fromList(
                mutationOperations,
                CrossOverOperation(
                    geneticOperators.crossMutation,
                    mutationConfig.geneticOperatorConfig.crossMutationProbability
                ),
                random
            )
        )
    }
}

fun test(simulationConfig: SimulationConfig, sensorPositions: List<Pair<Int, Int>>) {
    val simulationComponents = initializeSimulationComponents(simulationConfig)

}

