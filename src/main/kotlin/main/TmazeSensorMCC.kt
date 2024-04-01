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
import coevolution.*
import environment.*
import genome.ActivationFunction
import genome.NetworkGenome
import kotlin.collections.listOf
import kotlin.random.Random

private fun createCoefficients() = Coefficients(1.0, 1.0, 0.4)

private fun createMutationOperations(geneticOperators: GeneticOperators): List<MutationOperation> {
    return listOf(
        MutationOperation(0.04, geneticOperators.mutateAddConnection),
        MutationOperation(0.01, geneticOperators.mutateAddNode),
        MutationOperation(0.9, geneticOperators.mutateWeights),
        MutationOperation(0.10, geneticOperators.mutateActivationFunction),
        MutationOperation(0.05, geneticOperators.mutateConnectionEnabled)
    )
}


private fun createMutationOperationsMaze(geneticOperators: GeneticOperators): List<MutationOperation> {
    return listOf(
        MutationOperation(0.04, geneticOperators.mutateAddConnection),
        MutationOperation(0.01, geneticOperators.mutateAddNode),
        MutationOperation(0.9, geneticOperators.mutateWeights),
        MutationOperation(0.10, geneticOperators.mutateActivationFunction),
        MutationOperation(0.05, geneticOperators.mutateConnectionEnabled)
    )
}

fun main() {
    val random = Random(1)
    val populationSize = 300 // Adjusted for Iris dataset size
    val weightRange = -3.0..3.0
    val weight = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive)
    val weightMutationConfig = WeightMutationConfig(weight, .9, (-.1..0.1))
    // Step 3: Initialize components
    val nodeInnovationTracker = InnovationTracker()
    val connectionInnovationTracker = InnovationTracker()
    val agentInitialPopulationGenerator =
        tmazeAgentPopulationGenerator(
            weightRange,
            random,
            nodeInnovationTracker,
            connectionInnovationTracker,
            listOf(ActivationFunction.SIGMOID)
        )

    val networkProcessorFactory = NetworkProcessorFactory(NetworkBuilder(DefaultActivationFunctionMapper()))
    val mazeInitialPopulationGenerator =
        mazeCPPNPopulationGenerator(
            weightRange,
            random,
            nodeInnovationTracker,
            connectionInnovationTracker,
            ActivationFunction.cppn
        )
    val crossMutation = BiasedCrossover(random, 1.0)
    val mazeGeneticOperators =
        createDefaultGeneticOperators(
            crossMutation,
            ActivationFunction.cppn,
            random,
            nodeInnovationTracker,
            connectionInnovationTracker,
            RandomActivationFunctionSelection(random, ActivationFunction.cppn),
            weightMutationConfig, //true, true, true, true
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
    val mazeNetworkGenomeMutator = DefaultGenomeMutator(createMutationOperationsMaze(mazeGeneticOperators), random)
    val mazeGenomeMutator = SimpleMazeGenomeMutator(
        mazeNetworkGenomeMutator,
        random,
        createMutationParametersWithAdjustedRange(.1, .1, .1, .5, .5)
    )
    val agentGenomeMutator = DefaultGenomeMutator(createMutationOperations(agentGeneticOperator), random)
    val batchSize = 20
    val mazeBatchSize = 5
    val agentQueuePopulation = BatchQueuePopulation<Agent<NetworkGenome, MazeGenome>>(populationSize, batchSize).also {
        val individuals = agentInitialPopulationGenerator.generatePopulation(populationSize).map {
            MazeSolverAgent(it, agentGenomeMutator, networkProcessorFactory)
        }
        it.addToQueue(individuals)
    }

    val environmentQueuePopulation =
        BatchQueuePopulation<Environment<MazeGenome, NetworkGenome>>(populationSize, mazeBatchSize).also {
            val individuals = mazeInitialPopulationGenerator.generatePopulation(populationSize).map { networkGenome ->
                MazeEnvironmentAdapter(
                    mazeGenomeMutator = mazeGenomeMutator,
                    networkProcessorFactory = networkProcessorFactory,
                    mazeGenome = MazeGenome(
                        networkGenome = networkGenome,
                        mazeThresholds = MazeThresholds(.2, .5, .5),
                        width = 5,
                        height = 5
                    ),
                    resourceUsageLimit = 5
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
        val (agents, environments) = mccFramework.iterate()
        if (generation % 10 == 0) {
            println("Generation: $generation")
            println("Successful environments: ${environments.size}")
            println("Successful agents: ${agents.size}")
            if (generation % 50 == 0) {
                environments.forEach { environment ->
                    val mazeEnvironment = environment.getModel()
                    val networkProcessor = networkProcessorFactory.createProcessor(mazeEnvironment.networkGenome)
                    val mazeQuery = CPPNMazeQuery(networkProcessor)
                    val mazeGenerator =
                        CPPNMazeGenerator(mazeEnvironment.mazeThresholds, mazeEnvironment.width, mazeEnvironment.height)
                    val generatedMaze = mazeGenerator.generateMaze(mazeQuery)
                    if (generatedMaze != null) {
                        val mazeEnvironment = TmazeEnvironment(generatedMaze)
                        println("Successful environment:\n\n${renderEnvironmentAsString(mazeEnvironment)}")
                    }

                }
            }

        }
        generation++
    }
}


private fun mazeCPPNPopulationGenerator(
    weightRange: ClosedRange<Double>,
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
        randomWeight =
        GaussianRandomWeight(
            random,
            0.0,
            1.0,
            weightRange.start,
            weightRange.endInclusive
        ),
        nodeInnovationTracker,
        connectionInnovationTracker
    )
}

private
fun tmazeAgentPopulationGenerator(
    weightRange: ClosedRange<Double>,
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
        randomWeight =
        GaussianRandomWeight(
            random,
            0.0,
            1.0,
            weightRange.start,
            weightRange.endInclusive
        ),
        nodeInnovationTracker,
        connectionInnovationTracker
    )
}

