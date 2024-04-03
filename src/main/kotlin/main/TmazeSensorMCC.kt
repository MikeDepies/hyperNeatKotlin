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
import algorithm.fitnessevaluator.createSensorPositions
import coevolution.*
import environment.*
import genome.ActivationFunction
import genome.NetworkGenome
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.listOf
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

private fun createCoefficients() = Coefficients(1.0, 1.0, 0.4)

private fun createMutationOperations(geneticOperators: GeneticOperators, random: Random): GenomeMutatorConfig {
    return fromList(
        listOf(
            MutationOperation(0.04, geneticOperators.mutateAddConnection),
            MutationOperation(0.02, geneticOperators.mutateAddNode),
            MutationOperation(0.9, geneticOperators.mutateWeights),
            MutationOperation(0.01, geneticOperators.mutateActivationFunction),
            MutationOperation(0.05, geneticOperators.mutateConnectionEnabled)
        ), CrossOverOperation(geneticOperators.crossMutation, .9), random
    )
}


private fun createMutationOperationsMaze(geneticOperators: GeneticOperators, random: Random): GenomeMutatorConfig {
    return fromList(
        listOf(
            MutationOperation(0.2, geneticOperators.mutateAddConnection),
            MutationOperation(0.08, geneticOperators.mutateAddNode),
            MutationOperation(0.9, geneticOperators.mutateWeights),
            MutationOperation(0.1, geneticOperators.mutateActivationFunction),
            MutationOperation(0.15, geneticOperators.mutateConnectionEnabled)
        ), CrossOverOperation(geneticOperators.crossMutation, .8), random
    )
}

fun main() {
    val random = Random(1)
    val stepsAllowed = 100
    val populationSize = 50 // Adjusted for Iris dataset size
    val weightRange = -3.0..3.0
    val dispatcher = Executors.newFixedThreadPool(16).asCoroutineDispatcher()
    val weight = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive)
    val weightMutationConfig = WeightMutationConfig(weight, .9, (-.01..0.01))
    val weightMaze = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive)
    val weightMazeMutationConfig = WeightMutationConfig(weightMaze, .9, (-.03..0.03))
    // Step 3: Initialize components
    val nodeInnovationTracker = InnovationTracker()
    val connectionInnovationTracker = InnovationTracker()
    val sensorPositions = createSensorPositions(2)
    val agentInitialPopulationGenerator =
        tmazeAgentPopulationGenerator(
            weight,
            random,
            nodeInnovationTracker,
            connectionInnovationTracker,
            listOf(ActivationFunction.SIGMOID),
            sensorPositions
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
            weightMazeMutationConfig, //true, true, true, true
        )

    val agentGeneticOperator = createDefaultGeneticOperators(
        crossMutation,
        listOf(ActivationFunction.SIGMOID),
        random,
        nodeInnovationTracker,
        connectionInnovationTracker,
        SingleActivationFunctionSelection(ActivationFunction.SIGMOID),
        weightMutationConfig, true, true, true, true
    )
    val mazeNetworkGenomeMutator = DefaultGenomeMutator(createMutationOperationsMaze(mazeGeneticOperators, random))
    val mazeGenomeMutator = SimpleMazeGenomeMutator(
        mazeNetworkGenomeMutator,
        random,
        createMutationParametersWithAdjustedRange(.05, .05, .05, .0, .0)
    )
    val mazeEnvironmentCache = MazeEnvironmentCache(networkProcessorFactory, mazeGenomeMutator)
    val mazeAgentCache = MazeAgentCache(networkProcessorFactory)
    val agentGenomeMutator = DefaultGenomeMutator(createMutationOperations(agentGeneticOperator, random))
    val batchSize = 25
    val mazeBatchSize = 5
    val solutionChannel = Channel<SolutionMapCommand<NetworkGenome, MazeGenome>>()
    val solutionMapUpdater = SolutionMapUpdater(solutionChannel, mutableMapOf())
    val solutionMapCommandSender = SolutionMapCommandSender(solutionChannel)
    val solutionMap = DelegatedSolutionMap(solutionMapUpdater, solutionMapCommandSender)
    println(sensorPositions.size)
    val agentQueuePopulation = BatchQueuePopulation<Agent<NetworkGenome, MazeGenome>>(populationSize, batchSize).also {
        val individuals = agentInitialPopulationGenerator.generatePopulation(populationSize).map { networkGenome ->
            MazeSolverAgent(
                mazeAgentCache,
                mazeEnvironmentCache,
                networkGenome,
                agentGenomeMutator,
                solutionMap,
                stepsAllowed,
                sensorPositions
            )
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
                    solutionMap = solutionMap,
                    mazeGenome = MazeGenome(
                        networkGenome = networkGenome,
                        mazeThresholds = MazeThresholds(.6, .5, .5),
                        width = 64,
                        height = 24
                    ),
                    resourceUsageLimit = 5,
                    stepsAllowed = stepsAllowed,
                    sensorPositions = sensorPositions
                )
            }
            it.addToQueue(individuals)
        }
        
    val mccFramework = MCCFramework(
        agentQueuePopulation,
        environmentQueuePopulation,
        dispatcher
    )
    var generation = 0
    runBlocking {

        launch {
            solutionMapUpdater.init()
        }
        println("Initializing solution map")

        println("Solution map initialized")
        while (true) {
            solutionMap.clearSolutions()
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
                        val generatedMaze = mazeEnvironmentCache.getMazeEnvironment(environment.environment.getModel())
                        if (generatedMaze != null) {
                            val solution =
                                solutionMap.getSolution(environment.environment)?.minBy { it.solution.size }?.solution
                                    ?: emptyList()
                            val mazeEnvironment =
                                TmazeEnvironment(generatedMaze, generatedMaze.width, generatedMaze.height)
                            println(
                                "Successful environment:\n\n${
                                    renderEnvironmentAsString(
                                        mazeEnvironment,
                                        true,
                                        solution
                                    )
                                }"
                            )
                        }

                    }
                }

            }
            generation++
        }
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
        inputNodeCount = 3, // For x and y coordinates
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
    activationFunctions: List<ActivationFunction>,
    sensorPositions: List<Pair<Int, Int>>
): InitialPopulationGenerator {
    return SimpleInitialPopulationGenerator(
        inputNodeCount =
        2 + 2 + sensorPositions.size, // Adjusted for TMaze input (agent's x position, agent's y position, and
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

