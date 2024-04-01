package main

import algorithm.*
import algorithm.activation.SingleActivationFunctionSelection
import algorithm.crossover.BiasedCrossover
import algorithm.crossover.RandomCrossover
import algorithm.evolve.*
import algorithm.fitnessevaluator.MazeFitnessEvaluatorSensor
import algorithm.fitnessevaluator.SensorInputGenerator
import algorithm.weight.GaussianRandomWeight
import environment.RewardSide
import environment.TmazeEnvironment
import environment.createTMaze
import environment.hasPathToGoal
import environment.renderEnvironmentAsString
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
            MutationOperation(0.00, geneticOperators.mutateActivationFunction),
            // MutationOperation(0.05, geneticOperators.mutateConnectionEnabled)
    )
}

fun main() {
    val random = Random(1)
    val compatabilityThreshold = 2.3
    val populationSize = 500 // Adjusted for Iris dataset size
    val crossMutateChance = 0.5
    val weightRange = -3.0..3.0
    val weight = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive)
    val weightMutationConfig = WeightMutationConfig(weight, .9, (-.01..0.01))
    // Step 3: Initialize components
    val nodeInnovationTracker = InnovationTracker()
    val connectionInnovationTracker = InnovationTracker()
    val initialPopulationGenerator =
            tmazePopulationGeneratorSensor2(
                    weightRange,
                    random,
                    nodeInnovationTracker,
                    connectionInnovationTracker
            )
    val rewardSides = RewardSide.values().takeLast(1)
    val maps =
            (1..20).map {
                val rewardSide = rewardSides.random(random)
                createTMaze(rewardSide, random)
            }
    maps.forEachIndexed { index, maze ->
        val environment = TmazeEnvironment(maze)
        val hasPathToGoal = hasPathToGoal(environment)
        println("Maze ${index + 1}:\n${renderEnvironmentAsString(environment)}")
        println("Has path to goal: $hasPathToGoal")
    }
    val fitnessEvaluator =
            object : FitnessEvaluator {

                override fun calculateFitness(genome: NetworkGenome): Double {
                    var totalFitness = 0.0
                    val numberOfEvaluations = maps.size
                    maps.forEach { task ->
                        val environment = TmazeEnvironment(task)
                        val f =
                                MazeFitnessEvaluatorSensor(
                                        SensorInputGenerator(environment),
                                        environment
                                )
                        totalFitness += f.calculateFitness(genome)
                    }

                    return totalFitness / numberOfEvaluations
                }
            }
    val crossMutation = BiasedCrossover(random, 1.0)
    val geneticOperators =
            createDefaultGeneticOperators(
                    crossMutation,
                    listOf(ActivationFunction.SIGMOID),
                    random,
                    nodeInnovationTracker,
                    connectionInnovationTracker,
                    SingleActivationFunctionSelection(ActivationFunction.SIGMOID),
                    weightMutationConfig
            )
    val genomeMutator = DefaultGenomeMutator(createMutationOperations(geneticOperators), random)

    val genomeCompatibility = GenomeCompatibilityTraditional(createCoefficients())
    val speciation = SpeciationImpl(compatabilityThreshold, genomeCompatibility, random)
    val fitnessSharing = FitnessSharingExponential()


    val neatProcess =
            NEATProcessWithDirectReplacement(
                    initialPopulationGenerator,
                    fitnessEvaluator,
                    crossMutation,
                    genomeMutator,
                    speciation,
                    fitnessSharing,
                    populationSize,
                    crossMutateChance,
                    random,
                    0.2
            )

    // Step 5: Setup and run the NEAT algorithm
    var currentPopulation = emptyList<NetworkGenome>()
    val maxGenerations = 500
    for (generation in 1..maxGenerations) {
        print("Generation $generation: ")
        currentPopulation = neatProcess.executeGeneration(currentPopulation)
        // Step 6: Logging and observation
        val species = speciation.speciesList
        val maxFitness = currentPopulation.maxOfOrNull { it.fitness ?: Double.MIN_VALUE }
        val minFitness = currentPopulation.minOfOrNull { it.fitness ?: Double.MAX_VALUE }
        println(
                "Generation $generation: Max Fitness (${currentPopulation.count{ it.fitness == null}}) = $maxFitness, Species Count = ${species.size}"
        )
        species.filter { it.members.isNotEmpty() }.forEachIndexed { index, s ->
            val speciesMaxFitness = s.members.maxOfOrNull { it.fitness ?: 0.0 }
            val speciesMinFitness = s.members.minOfOrNull { it.fitness ?: Double.MAX_VALUE }
            println(
                    "Species $index (${s.id})P-${s.members.size}: Best Fitness = $speciesMaxFitness, Worst Fitness = $speciesMinFitness"
            )
        }
        // Add your condition to check for an acceptable solution and break if found
        //        if (maxFitness != null && maxFitness > 1.5) { // Break condition for high fitness
        //            println("High fitness achieved. Stopping...")
        //            break
        //        }
    }
}

private fun tmazePopulationGeneratorSensor2(
        weightRange: ClosedRange<Double>,
        random: Random,
        nodeInnovationTracker: InnovationTracker,
        connectionInnovationTracker: InnovationTracker,
): InitialPopulationGenerator {
    return SimpleInitialPopulationGenerator(
            inputNodeCount =
                    7, // Adjusted for TMaze input (agent's x position, agent's y position, and
            // reward side)
            outputNodeCount = 4, // Adjusted for TMaze actions (MOVE_FORWARD, MOVE_LEFT, MOVE_RIGHT, MOVE_BACKWARD)
            hiddenNodeCount = 0, // No hidden nodes initially
            connectionDensity = 1.0,
            activationFunctions =
                    listOf(ActivationFunction.SIGMOID), // Using SIGMOID for activation
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
