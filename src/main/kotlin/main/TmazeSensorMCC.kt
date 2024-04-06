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
import kotlinx.coroutines.Dispatchers
import java.lang.Integer.max
import java.util.concurrent.Executors
import kotlin.math.max

private fun createCoefficients() = Coefficients(1.0, 1.0, 0.4)

private fun createMutationOperations(geneticOperators: GeneticOperators, random: Random): GenomeMutatorConfig {
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


private fun createMutationOperationsMaze(geneticOperators: GeneticOperators, random: Random): GenomeMutatorConfig {
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

fun main() =runBlocking(Dispatchers.Default) {
    val random = Random(1)
    val resourceUsageLimit = 3
    val stepsAllowed = 100
    val populationSize = 100 // Adjusted for Iris dataset size
    val mazePopulationSize = 80
    val weightRange = -3.0..3.0
    
    val dispatcher = Executors.newFixedThreadPool(16).asCoroutineDispatcher()
    val weight = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive)
    val weightMutationConfig = WeightMutationConfig(weight, .9, (-.01..0.01))
    val weightMaze = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive)
    val weightMazeMutationConfig = WeightMutationConfig(weightMaze, .6, (-.01..0.01))
    // Step 3: Initialize components
    val nodeInnovationTracker = InnovationTracker()
    val connectionInnovationTracker = InnovationTracker()
    val sensorPositions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))//, Pair(2, 0), Pair(-2, 0), Pair(0, 2), Pair(0, -2))//createSensorPositions(1)
    val agentInitialPopulationGenerator =
        tmazeAgentPopulationGenerator(
            weight,
            random,
            nodeInnovationTracker,
            connectionInnovationTracker,
            listOf(ActivationFunction.SIGMOID),
            sensorPositions
        )

    val networkProcessorFactory = NetworkProcessorFactory(NetworkBuilder(DefaultActivationFunctionMapper()), true, 10, 0.01)
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
            weightMazeMutationConfig, true, true, true, true
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
        createMutationParametersWithAdjustedRange(.0, .0, .0, .0, .0)
    )
    val mazeEnvironmentCache = MazeEnvironmentCache(networkProcessorFactory, mazeGenomeMutator)
    val mazeAgentCache = MazeAgentCache(networkProcessorFactory)
    val agentGenomeMutator = DefaultGenomeMutator(createMutationOperations(agentGeneticOperator, random))
    val batchSize = 50
    val mazeBatchSize = 5
    val solutionChannel = Channel<SolutionMapCommand<NetworkGenome, MazeGenome>>(Channel.UNLIMITED)
    val solutionMapUpdater = SolutionMapUpdater(solutionChannel, mutableMapOf())
    val solutionMapCommandSender = SolutionMapCommandSender(solutionChannel)
    val solutionMap = DelegatedSolutionMap(solutionMapUpdater, solutionMapCommandSender)
    println(sensorPositions.size)
    val agentQueuePopulation = BatchQueuePopulation<Agent<NetworkGenome, MazeGenome>>(populationSize, batchSize).also {
        val individuals = agentInitialPopulationGenerator.generatePopulation(populationSize).map { networkGenome ->
            MazeSolverAgent(
                random,
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
        BatchQueuePopulation<Environment<MazeGenome, NetworkGenome>>(mazePopulationSize, mazeBatchSize).also {
            val individuals = mazeInitialPopulationGenerator.generatePopulation(mazePopulationSize / 5).map { networkGenome ->
                MazeEnvironmentAdapter(
                    dispatcher = dispatcher,
                    mazeAgentCache = mazeAgentCache,
                    mazeEnvironmentCache = mazeEnvironmentCache,
                    mazeGenomeMutator = mazeGenomeMutator,
                    solutionMap = solutionMap,
                    mazeGenome = MazeGenome(
                        networkGenome = networkGenome,
                        mazeThresholds = MazeThresholds(.5, .5, .5),
                        width = 36,
                        height = 12
                    ),
                    resourceUsageLimit = resourceUsageLimit,
                    stepsAllowed = stepsAllowed,
                    sensorPositions = sensorPositions
                )
            }
            it.addToQueue(individuals)
        }
    
    
    val mccFramework = MCCFramework(
        random,
        agentQueuePopulation,
        environmentQueuePopulation,
        dispatcher,
        maxAgents = agentQueuePopulation.queue.size.toDouble() * .02,
        minAgents = 1.0,
        environmentMCTest = { mccFramework, solvedEnvironment -> 
            solvedEnvironment.agents.size in (mccFramework.minAgents.toInt()..mccFramework.maxAgents.toInt())
//             solvedEnvironment.agents.size in (mccFramework.minAgents.toInt()..mccFramework.maxAgents.toInt())
        }
    )
    var generation = 0
    

        launch(Dispatchers.Default) {
            solutionMapUpdater.init()
        }
        while (true) {
            solutionMap.clearSolutions()
            if (generation % 500 == 0) {
                mazeAgentCache.clearCache()
                mazeEnvironmentCache.clearCache()
            }
           val (agents, environments) = mccFramework.iterate()
            val resourceUsageCount = environmentQueuePopulation.queue.sumOf { it.resourceUsageCount }
            if (generation % 100 == 0) {
                report(generation, environments, agents, resourceUsageCount, mccFramework, agentQueuePopulation, environmentQueuePopulation)
                if (generation % 10 == 0) {
                    environments.forEach { environment ->
                        val generatedMaze = mazeEnvironmentCache.getMazeEnvironment(environment.environment.getModel())
                        if (generatedMaze != null) {
                            val solution =
                                solutionMap.getSolution(environment.environment)?.map { it.solution }?.sortedBy { it.size }//?.minBy { it.solution.size }?.solution
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

fun report(generation: Int, environments: List<SolvedEnvironment<MazeGenome, NetworkGenome>>, agents: List<Agent<NetworkGenome, MazeGenome>>, resourceUsageCount: Int, mccFramework: MCCFramework<NetworkGenome, MazeGenome>, agentQueuePopulation: BatchQueuePopulation<Agent<NetworkGenome, MazeGenome>>, environmentQueuePopulation: BatchQueuePopulation<Environment<MazeGenome, NetworkGenome>>) {
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

