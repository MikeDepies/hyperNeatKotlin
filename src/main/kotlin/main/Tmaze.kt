package main

import algorithm.*
import algorithm.activation.SingleActivationFunctionSelection
import algorithm.crossover.RandomCrossover
import algorithm.crossover.BiasedCrossover
import genome.ActivationFunction
import genome.NetworkGenome
import algorithm.evolve.*
import algorithm.fitnessevaluator.SensorInputGenerator
import algorithm.weight.GaussianRandomWeight
import algorithm.fitnessevaluator.TmazeFitnessEvaluator
import environment.RewardSide
import environment.TmazeEnvironment
import environment.createTMaze
import environment.renderEnvironmentAsString
import kotlin.random.Random

private fun createCoefficients() = Coefficients(1.0, 1.0, 0.4)

private fun createMutationOperations(geneticOperators: GeneticOperators, random: Random): GenomeMutatorOperationConfig {
    return fromList(listOf(
        MutationOperation(0.04, geneticOperators.mutateAddConnection),
        MutationOperation(0.01, geneticOperators.mutateAddNode),
        MutationOperation(0.9, geneticOperators.mutateWeights),
        MutationOperation(0.04, geneticOperators.mutateActivationFunction),
        MutationOperation(0.05, geneticOperators.mutateConnectionEnabled)
    ), CrossOverOperation(geneticOperators.crossMutation, .7), random)
}
fun main() {
    val random = Random(0)
    val weightRange = -3.0..3.0
    val weight = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive)
    val weightMutationConfig = WeightMutationConfig(weight, .7, (-.1..0.1))
    // Step 3: Initialize components
    val nodeInnovationTracker = InnovationTracker()
    val connectionInnovationTracker = InnovationTracker()
    val initialPopulationGenerator = tmazePopulationGenerator(weightRange, random, nodeInnovationTracker, connectionInnovationTracker)
    val rewardSide = RewardSide.values().random(random)
    val task = createTMaze(rewardSide, random)
    val environment = TmazeEnvironment(task, 9, 6)

    val fitnessEvaluator = object : FitnessEvaluator {
        val f = TmazeFitnessEvaluator(environment)
        override fun calculateFitness(genome: NetworkGenome): Double {
//            f.environment.reset()
//            println("Initial state\n ${renderEnvironmentAsString(f.environment)}\n")
            val fitness = f.calculateFitness(genome)
//            println("Fitness: $fitness\n ${renderEnvironmentAsString(f.environment)}\n")
            return fitness
        }
    }
    val crossMutation = RandomCrossover(random)
    val geneticOperators = createDefaultGeneticOperators(
        crossMutation,

        random,
        nodeInnovationTracker,
        connectionInnovationTracker,
        SingleActivationFunctionSelection(ActivationFunction.SIGMOID),
        weightMutationConfig,
        false, false, false, false
    )
    val genomeMutator = DefaultGenomeMutator(
        createMutationOperations(geneticOperators, random)
    )
    val compatabilityThreshold = 3.0
    val genomeCompatibility = GenomeCompatibilityTraditional(createDefaultCoefficients())
    val speciation = SpeciationImpl(compatabilityThreshold, genomeCompatibility, random)
    val fitnessSharing = FitnessSharingExponential()
    val populationSize = 150 // Adjusted for Iris dataset size
    val crossMutateChance = 0.9

    val neatProcess = NEATProcessWithDirectReplacement(
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
    val maxGenerations = 500 // Adjusted for complexity of Iris problem
    for (generation in 1..maxGenerations) {
        print("Generation $generation: ")
        currentPopulation = neatProcess.executeGeneration(currentPopulation)
        // Step 6: Logging and observation
        val species = speciation.speciesList
        val maxFitness = currentPopulation.maxOfOrNull { it.fitness ?: 0.0 }
        val minFitness = currentPopulation.minOfOrNull { it.fitness ?: Double.MAX_VALUE }
        println(
            "Generation $generation: Max Fitness (${currentPopulation.count{ it.fitness == null}}) = $maxFitness, Species Count = ${species.size}"
        )
        species.filter { it.members.isNotEmpty()}.forEachIndexed { index, s ->
            val speciesMaxFitness = s.members.maxOfOrNull { it.fitness ?: 0.0 }
            val speciesMinFitness = s.members.minOfOrNull { it.fitness ?: Double.MAX_VALUE }
            println("Species $index: Best Fitness = $speciesMaxFitness, Worst Fitness = $speciesMinFitness")
        }
        // Add your condition to check for an acceptable solution and break if found
        if (maxFitness != null && maxFitness > 1.5) { // Break condition for high fitness
            println("High fitness achieved. Stopping...")
            break
        }
    }
}
private fun tmazePopulationGenerator(weightRange: ClosedRange<Double>, random: Random,
nodeInnovationTracker: InnovationTracker,
connectionInnovationTracker: InnovationTracker,): InitialPopulationGenerator {
    return SimpleInitialPopulationGenerator(
        inputNodeCount = 3, // Adjusted for TMaze input (agent's x position, agent's y position, and reward side),
        outputNodeCount = 3, // Adjusted for TMaze actions (MOVE_FORWARD, MOVE_LEFT, MOVE_RIGHT)
        hiddenNodeCount = 0, // No hidden nodes initially
        connectionDensity = 1.0,
        activationFunctions = listOf(ActivationFunction.SIGMOID), // Using SIGMOID for activation
        random = random,
        randomWeight = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive),
        nodeInnovationTracker = nodeInnovationTracker,
        connectionInnovationTracker = connectionInnovationTracker
    )
}

