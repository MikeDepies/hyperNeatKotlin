package main

import algorithm.*
import algorithm.activation.RandomActivationFunctionSelection
import algorithm.activation.SingleActivationFunctionSelection
import algorithm.createMutationOperations
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import main.mccMaze.*
import java.lang.Integer.max
import java.util.concurrent.Executors
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import HyperNEAT
import Substrate
import DefaultNodeTypeResolver
import DefaultPointNormalizer
import DefaultSubstrateFilter
import createSubstrate
import genome.NodeType

private fun createCoefficients() = Coefficients(1.0, 1.0, 0.4)

fun main() = runBlocking(Dispatchers.Default) {
    //listOf(Pair(1,0), Pair(-1,0), Pair(0,1), Pair(0,-1))//
    val sensorPositions = listOf(Position(1,0), Position(-1,0), Position(0,1), Position(0,-1))//createSensorPositions(3)
    val outputPositions = listOf(Position(0,1), Position(-1,0), Position(1,0), Position(0,-1))
    val simulationConfig = initializeSimulationConfig(sensorPositions)
    val simulationComponents = initializeSimulationComponents(simulationConfig)

    val agentInitialPopulationGenerator =
        tmazeAgentPopulationGenerator(
            simulationConfig.mutationOperations,
            simulationComponents.weightMutationConfig.randomWeight,
            simulationComponents.random,
            simulationComponents.nodeInnovationTracker,
            simulationComponents.connectionInnovationTracker,
            simulationConfig.mutationOperations.activationFunctions
        )

    val networkProcessorFactory =
        NetworkProcessorFactory(NetworkBuilder(DefaultActivationFunctionMapper()), false, 10, 0.01)
    val mazeInitialPopulationGenerator =
        mazeCPPNPopulationGenerator(
            simulationConfig.mazeMutationOperations,
            simulationComponents.weightMutationConfig.randomWeight,
            simulationComponents.random,
            simulationComponents.mazeNodeInnovationTracker,
            simulationComponents.mazeConnectionInnovationTracker
        )

    val mazeNetworkGenomeMutator = MutationOperationFactory(
        simulationComponents.random,
        simulationConfig.mazeMutationOperations.mutationConfig
    ).createFromConfig(simulationComponents.weightMazeMutationConfig)
    val mazeGenomeMutator = SimpleMazeGenomeMutator(
        mazeNetworkGenomeMutator,
        simulationComponents.random,
        createMutationParametersWithAdjustedRange(.0, .0, .0, .0, .0)
    )
    val mazeEnvironmentCache = MazeEnvironmentCache(networkProcessorFactory, mazeGenomeMutator)
    val pointNormalizer = DefaultPointNormalizer(5, 5, 5)
    val defaultSubstrateFilter = DefaultSubstrateFilter { 
        val pos = Position(it.indexes[0], it.indexes[1])
        (it.indexes[2] == 0 && pos in sensorPositions || it.indexes[2] == pointNormalizer.depth - 1 && pos !in outputPositions).also { b ->
            print("Filter ${it.indexes} $b")
         }  }
    val substrate = createSubstrate(pointNormalizer, pointNormalizer.width, pointNormalizer.height, pointNormalizer.depth, defaultSubstrateFilter)
    val nodeTypeResolver = DefaultNodeTypeResolver {
        when {
            it.indexes[2] == 0 -> NodeType.INPUT
            it.indexes[2] == pointNormalizer.depth - 1 -> NodeType.OUTPUT
            else -> NodeType.HIDDEN
        }.also { type ->println("${it.indexes} ${type}") }
    }
    val hyperNeat = HyperNEAT(substrate, nodeTypeResolver)
    val hyperNEATCache = HyperNEATAgentCache(networkProcessorFactory, hyperNeat)
    val mazeAgentCache = MazeAgentCache(networkProcessorFactory)
    val agentGenomeMutator = MutationOperationFactory(
        simulationComponents.random,
        simulationConfig.mutationOperations.mutationConfig
    ).createFromConfig(simulationComponents.weightMutationConfig)
    val solutionChannel = Channel<SolutionMapCommand<NetworkGenome, MazeGenome>>(Channel.UNLIMITED)
    val solutionMapUpdater = SolutionMapUpdater(solutionChannel, mutableMapOf())
    val solutionMapCommandSender = SolutionMapCommandSender(solutionChannel)
    val solutionMap = DelegatedSolutionMap(solutionMapUpdater, solutionMapCommandSender)
    val agentQueuePopulation =
        BatchQueuePopulation<Agent<NetworkGenome, MazeGenome>>(
            simulationConfig.mutationOperations.size,
            simulationConfig.mutationOperations.batchSize
        ).also {
            val individuals =
                agentInitialPopulationGenerator.generatePopulation(simulationConfig.mutationOperations.size)
                    .map { networkGenome ->

                        MazeSolverAgent(
                            simulationComponents.random,
                            mazeAgentCache,
                            mazeEnvironmentCache,
                            networkGenome,
                            agentGenomeMutator,
                            solutionMap,
                            simulationConfig.stepsAllowed,
                            sensorPositions
                        )
                    }
            it.addToQueue(individuals)
        }

    val environmentQueuePopulation = BatchQueuePopulation<Environment<MazeGenome, NetworkGenome>>(
        simulationConfig.mazeMutationOperations.size,
        simulationConfig.mazeMutationOperations.batchSize
    ).also {
        val individuals =
            mazeInitialPopulationGenerator.generatePopulation(simulationConfig.mazeMutationOperations.size)
                .map { networkGenome ->

                    MazeEnvironmentAdapter(
                        dispatcher = simulationComponents.dispatcher,
                        mazeAgentCache = mazeAgentCache,
                        mazeEnvironmentCache = mazeEnvironmentCache,
                        mazeGenomeMutator = mazeGenomeMutator,
                        solutionMap = solutionMap,
                        mazeGenome = MazeGenome(
                            networkGenome = networkGenome,
                            mazeThresholds = simulationConfig.mazeDefinition.mazeThresholds,
                            width = simulationConfig.mazeDefinition.width,
                            height = simulationConfig.mazeDefinition.height
                        ),
                        resourceUsageLimit = simulationConfig.resourceUsageLimit,
                        stepsAllowed = simulationConfig.stepsAllowed,
                        sensorPositions = sensorPositions
                    )
                }
        it.addToQueue(individuals)
    }
    var generation = 0
    val factory = MccFrameworkFactory(
        simulationConfig,
        simulationComponents,
        sensorPositions,
        agentInitialPopulationGenerator,
        mazeInitialPopulationGenerator,
        mazeEnvironmentCache,
        mazeAgentCache,
        agentGenomeMutator,
        mazeGenomeMutator
    )
    { mccFramework, solvedEnvironment ->
        val maze = mazeEnvironmentCache.getMazeEnvironment(solvedEnvironment.environment.getModel())
        val shortestPath = maze?.let { shortestPathToGoal(TmazeEnvironment(maze, maze.width, maze.height)) } ?: 0
        // val req = log2(generation.toDouble()).coerceAtLeast(1.0) 

        //shortestPath < req && 
        solvedEnvironment.agents != null && solvedEnvironment.agents.size in (mccFramework.minAgents.toInt()..mccFramework.maxAgents.toInt())
    }
    val mccFramework = factory.create(agentQueuePopulation, environmentQueuePopulation)


    var mode = 0
    launch(Dispatchers.Default) {
        solutionMapUpdater.init()
    }
    while (true) {
        solutionMap.clearSolutions()
        if (generation % 500 == 0) {
            mazeAgentCache.clearCache()
            mazeEnvironmentCache.clearCache()
        }
        if (generation % 150 < 40) {
            mode = 0
        } else {
            mode = 1
        }
        when (mode) {
            0 -> {
                mccFramework.minAgents = 1.0
                mccFramework.maxAgents = 50.0
            }

            1 -> {
                mccFramework.minAgents = 1.0
                mccFramework.maxAgents = 1.0
            }
        }

        val (agents, environments) = mccFramework.iterate()
        val resourceUsageCount = environmentQueuePopulation.queue.sumOf { it.resourceUsageCount }
        if (generation % 100 == 0) {
            report(
                generation,
                environments,
                agents,
                resourceUsageCount,
                mccFramework,
                agentQueuePopulation,
                environmentQueuePopulation
            )
            if (generation % 10 == 0) {

                environments.forEach { environment ->
                    val generatedMaze = mazeEnvironmentCache.getMazeEnvironment(environment.environment.getModel())
                    if (generatedMaze != null) {
                        val solution =
                            solutionMap.getSolution(environment.environment)?.map { it.solution }
                                ?.sortedBy { it.size }//?.minBy { it.solution.size }?.solution
                                ?: emptyList()
                        val mazeEnvironment =
                            TmazeEnvironment(generatedMaze, generatedMaze.width, generatedMaze.height)
                        println(
                            "Successful environment:\n(${solution.size})\n${
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

fun report(
    generation: Int,
    environments: List<SolvedEnvironment<MazeGenome, NetworkGenome>>,
    agents: List<Agent<NetworkGenome, MazeGenome>>,
    resourceUsageCount: Int,
    mccFramework: MCCFramework<NetworkGenome, MazeGenome>,
    agentQueuePopulation: BatchQueuePopulation<Agent<NetworkGenome, MazeGenome>>,
    environmentQueuePopulation: BatchQueuePopulation<Environment<MazeGenome, NetworkGenome>>
) {
    println("Generation: $generation")
    println("Successful environments: ${environments.size}")
    println("Successful agents: ${agents.size}")
    println("Resource usage: ${resourceUsageCount}")
    println("Constraint: minAgents: ${mccFramework.minAgents}, maxAgents: ${mccFramework.maxAgents}")
    println("================")
    println("Total Agent Population: ${agentQueuePopulation.queue.size}")
    println("Total Agent Population Genomes: ${agentQueuePopulation.queue.sumOf { it.getModel().nodeGenomes.size }} - ${agentQueuePopulation.queue.sumOf { it.getModel().connectionGenes.size }}")
    println("Average Agent Population Genomes: ${agentQueuePopulation.queue.sumOf { it.getModel().nodeGenomes.size } / agentQueuePopulation.queue.size.toDouble()} - ${agentQueuePopulation.queue.sumOf { it.getModel().connectionGenes.size } / agentQueuePopulation.queue.size.toDouble()}")
    println("================")
    println("Total Environment Population: ${environmentQueuePopulation.queue.size}")
    println("Total Environment Population Genomes: ${environmentQueuePopulation.queue.sumOf { it.getModel().networkGenome.nodeGenomes.size }} - ${environmentQueuePopulation.queue.sumOf { it.getModel().networkGenome.connectionGenes.size }}")
    println("Average Environment Population Genomes: ${environmentQueuePopulation.queue.sumOf { it.getModel().networkGenome.nodeGenomes.size } / environmentQueuePopulation.queue.size.toDouble()} - ${environmentQueuePopulation.queue.sumOf { it.getModel().networkGenome.connectionGenes.size } / environmentQueuePopulation.queue.size.toDouble()}")
    println("${log2(generation.toDouble()).coerceAtLeast(1.0)}")
}


private fun mazeCPPNPopulationGenerator(
    populationConfig: PopulationConfig,
    randomWeight: RandomWeight,
    random: Random,
    nodeInnovationTracker: InnovationTracker,
    connectionInnovationTracker: InnovationTracker
): InitialPopulationGenerator {
    return SimpleInitialPopulationGenerator(
        inputNodeCount = populationConfig.inputNodes, // For x and y coordinates
        outputNodeCount = populationConfig.outputNodes, // For wall, agent start, and goal position probabilities
        hiddenNodeCount = populationConfig.hiddenNodes, // No hidden nodes initially, adjust as needed for complexity
        connectionDensity = 1.0, // Full connection density, adjust as needed
        activationFunctions = populationConfig.activationFunctions,
        random = random,
        randomWeight = randomWeight,
        nodeInnovationTracker,
        connectionInnovationTracker
    )
}

private
fun tmazeAgentPopulationGenerator(
    populationConfig: PopulationConfig,
    randomWeight: RandomWeight,
    random: Random,
    nodeInnovationTracker: InnovationTracker,
    connectionInnovationTracker: InnovationTracker,
    activationFunctions: List<ActivationFunction>
): InitialPopulationGenerator {
    return SimpleInitialPopulationGenerator(
        inputNodeCount =
        populationConfig.inputNodes, // Adjusted for TMaze input (agent's x position, agent's y position, and
        // reward side)
        outputNodeCount = populationConfig.outputNodes, // Adjusted for TMaze actions (MOVE_FORWARD, MOVE_LEFT, MOVE_RIGHT, MOVE_BACKWARD)
        hiddenNodeCount = populationConfig.hiddenNodes, // No hidden nodes initially
        connectionDensity = 1.0,
        activationFunctions = activationFunctions,
        random = random,
        randomWeight = randomWeight,
        nodeInnovationTracker,
        connectionInnovationTracker
    )
}

