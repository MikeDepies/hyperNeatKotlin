package main

import algorithm.*
import algorithm.activation.RandomActivationFunctionSelection
import algorithm.activation.SingleActivationFunctionSelection
import algorithm.crossover.BiasedCrossover
import algorithm.evolve.*
import algorithm.network.DefaultActivationFunctionMapper
import algorithm.network.NetworkBuilder
import algorithm.network.NetworkProcessorFactory
import algorithm.weight.GaussianRandomWeight
import algorithm.weight.RandomWeight
import algorithm.weight.SimpleRandomWeight
import coevolution.*
import environment.*
import genome.ActivationFunction
import genome.NetworkGenome
import kotlin.collections.listOf
import kotlin.random.Random

private fun createCoefficients() = Coefficients(1.0, 1.0, 0.4)

private fun createMutationOperations(geneticOperators: GeneticOperators, random: Random): GenomeMutatorConfig {
    return fromList(listOf(
        MutationOperation(0.04, geneticOperators.mutateAddConnection),
        MutationOperation(0.02, geneticOperators.mutateAddNode),
        MutationOperation(0.9, geneticOperators.mutateWeights),
        MutationOperation(0.01, geneticOperators.mutateActivationFunction),
        MutationOperation(0.05, geneticOperators.mutateConnectionEnabled)
    ), CrossOverOperation(geneticOperators.crossMutation,.5), random)
}


private fun createMutationOperationsMaze(geneticOperators: GeneticOperators, random: Random): GenomeMutatorConfig {
    return fromList(listOf(
        MutationOperation(0.02, geneticOperators.mutateAddConnection),
        MutationOperation(0.01, geneticOperators.mutateAddNode),
        MutationOperation(0.9, geneticOperators.mutateWeights),
        MutationOperation(0.01, geneticOperators.mutateActivationFunction),
        MutationOperation(0.05, geneticOperators.mutateConnectionEnabled)
    ), CrossOverOperation(geneticOperators.crossMutation, .5), random)
}

fun main() {
    val random = Random(1)
    val stepsAllowed = 32
    val populationSize = 100 // Adjusted for Iris dataset size
    val weightRange = -3.0..3.0
// 
    val weight = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive)
    val weightMutationConfig = WeightMutationConfig(weight, .9, (-.91..0.91))
    val weightMaze = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive)
    val weightMazeMutationConfig = WeightMutationConfig(weightMaze, .9, (-.3..0.3))
    // Step 3: Initialize components
    val nodeInnovationTracker = InnovationTracker()
    val connectionInnovationTracker = InnovationTracker()
    val agentInitialPopulationGenerator =
        tmazeAgentPopulationGenerator(
            weight,
            random,
            nodeInnovationTracker,
            connectionInnovationTracker,
            listOf(ActivationFunction.SIGMOID)
        )

    val networkProcessorFactory = NetworkProcessorFactory(NetworkBuilder(DefaultActivationFunctionMapper()), 2)
    val mazeInitialPopulationGenerator =
        mazeCPPNPopulationGenerator(
            weight,
            random,
            nodeInnovationTracker,
            connectionInnovationTracker,
            ActivationFunction.cppn
        )
    val crossMutation = BiasedCrossover(random)
    val mazeGeneticOperators =
        createDefaultGeneticOperators(
            crossMutation,
            ActivationFunction.cppn,
            random,
            nodeInnovationTracker,
            connectionInnovationTracker,
            RandomActivationFunctionSelection(random, ActivationFunction.cppn),
            weightMazeMutationConfig//, true, true, true, true
        )

    val agentGeneticOperator = createDefaultGeneticOperators(
        crossMutation,
        listOf(ActivationFunction.SIGMOID),
        random,
        nodeInnovationTracker,
        connectionInnovationTracker,
        SingleActivationFunctionSelection(ActivationFunction.SIGMOID),
        weightMutationConfig
    )
    val mazeNetworkGenomeMutator = DefaultGenomeMutator(createMutationOperationsMaze(mazeGeneticOperators, random))
    val mazeGenomeMutator = SimpleMazeGenomeMutator(
        mazeNetworkGenomeMutator,
        random,
        createMutationParametersWithAdjustedRange(.0, .0, .0, .0, .0)
    )
    val mazeEnvironmentCache = MazeEnvironmentCache(networkProcessorFactory, mazeGenomeMutator)
    val mazeAgentCache = MazeAgentCache(networkProcessorFactory)
    val agentGenomeMutator = DefaultGenomeMutator(createMutationOperations(agentGeneticOperator, random))
    val batchSize = 25
    val mazeBatchSize = 25
    val agentQueuePopulation = BatchQueuePopulation<Agent<NetworkGenome, MazeGenome>>(populationSize, batchSize).also {
        val individuals = agentInitialPopulationGenerator.generatePopulation(populationSize).map { networkGenome ->     
            MazeSolverAgent(mazeAgentCache, mazeEnvironmentCache, networkGenome, agentGenomeMutator, stepsAllowed)
        }
        it.addToQueue(individuals)
    }

    val environmentQueuePopulation =
        BatchQueuePopulation<Environment<MazeGenome, NetworkGenome>>(populationSize, mazeBatchSize).also {
            val individuals = mazeInitialPopulationGenerator.generatePopulation(populationSize).map { networkGenome ->
                MazeEnvironmentAdapter( 
                    mazeAgentCache = mazeAgentCache,
                    mazeEnvironmentCache = mazeEnvironmentCache,
                    mazeGenomeMutator = mazeGenomeMutator,

                    mazeGenome = MazeGenome(
                        networkGenome = networkGenome,
                        mazeThresholds = MazeThresholds(.2, .5, .5),
                        width = 32,
                        height = 8
                    ),
                    resourceUsageLimit = 5,
                    stepsAllowed = stepsAllowed
                )
            }
            it.addToQueue(individuals)
        }
    val mccFramework = MCCFramework(
        agentQueuePopulation,
        environmentQueuePopulation
    )
    var generation = 0
    while (true) {
        if (generation % 500 == 0) {
                mazeAgentCache.clearCache()
                mazeEnvironmentCache.clearCache()
        }
        val (agents, environments) = mccFramework.iterate()
        if (generation % 10 == 0) {
            println("Generation: $generation")
            println("Successful environments: ${environments.size}")
            println("Successful agents: ${agents.size}")
            println("Resource usage: ${environmentQueuePopulation.queue.sumOf { it.resourceUsageCount }}")
            if (generation % 10 == 0) {
                environments.forEach { environment ->
                    val generatedMaze = mazeEnvironmentCache.getMazeEnvironment(environment.getModel())
                    if (generatedMaze != null) {
                        val mazeEnvironment = TmazeEnvironment(generatedMaze, generatedMaze.width, generatedMaze.height)
                        println("Successful environment:\n\n${renderEnvironmentAsString(mazeEnvironment, true)}")
                    }

                }
            }

        }
        generation++
    }
}


private fun mazeCPPNPopulationGenerator(
    randomWeight: RandomWeight,
    random: Random,
    nodeInnovationTracker: InnovationTracker,
    connectionInnovationTracker: InnovationTracker,
    activationFunctions: List<ActivationFunction>
): InitialPopulationGenerator {
    return SimpleInitialPopulationGenerator(
        inputNodeCount = 2, // For x and y coordinates
        outputNodeCount = 3, // For wall, agent start, and goal position probabilities
        hiddenNodeCount = 0, // No hidden nodes initially, adjust as needed for complexity
        connectionDensity = 1.0, // Full connection density, adjust as needed
        activationFunctions = activationFunctions,
        random = random,
        randomWeight = randomWeight,
        nodeInnovationTracker,
        connectionInnovationTracker
    )
}

private
fun tmazeAgentPopulationGenerator(
    randomWeight: RandomWeight,
    random: Random,
    nodeInnovationTracker: InnovationTracker,
    connectionInnovationTracker: InnovationTracker,
    activationFunctions: List<ActivationFunction>
): InitialPopulationGenerator {
    return SimpleInitialPopulationGenerator(
        inputNodeCount =
        8, // Adjusted for TMaze input (agent's x position, agent's y position, and
        // reward side)
        outputNodeCount = 4, // Adjusted for TMaze actions (MOVE_FORWARD, MOVE_LEFT, MOVE_RIGHT, MOVE_BACKWARD)
        hiddenNodeCount = 0, // No hidden nodes initially
        connectionDensity = 1.0,
        activationFunctions = activationFunctions,
        random = random,
        randomWeight = randomWeight,
        nodeInnovationTracker,
        connectionInnovationTracker
    )
}

