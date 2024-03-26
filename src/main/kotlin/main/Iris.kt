package main

import algorithm.fitnessevaluator.IrisFitnessEvaluator
import algorithm.activation.SingleActivationFunctionSelection
import algorithm.crossover.RandomCrossover
import algorithm.crossover.BiasedCrossover
import genome.ActivationFunction
import genome.NetworkGenome
import algorithm.evolve.*
import algorithm.weight.GaussianRandomWeight
import algorithm.createDefaultGeneticOperators
import algorithm.InnovationTracker
import algorithm.DefaultGenomeMutator
import algorithm.createMutationOperations
import kotlin.random.Random

fun main() {
    val random = Random(0)
    val weightRange = -30.0..30.0
    // Step 3: Initialize components
    val initialPopulationGenerator = irisPopulationGenerator(weightRange, random)
    val fitnessEvaluator = IrisFitnessEvaluator(random)
    val crossMutation = RandomCrossover(random)
    val geneticOperators = createDefaultGeneticOperators(
        weightRange,
        listOf(ActivationFunction.SIGMOID),
        random,
        InnovationTracker(),
        InnovationTracker(),
        SingleActivationFunctionSelection(ActivationFunction.SIGMOID)
    )
    val genomeMutator = DefaultGenomeMutator(
        createMutationOperations(geneticOperators),
        random
    )
    val compatabilityThreshold = 1.0
    val genomeCompatibility = GenomeCompatibilityTraditional(createDefaultCoefficients())
    val speciation = SpeciationImpl(compatabilityThreshold, genomeCompatibility, random)
    val fitnessSharing = FitnessSharingExponential()
    val populationSize = 150 // Adjusted for Iris dataset size
    val crossMutateChance = 0.1

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
        println("Generation $generation: Max Fitness = $maxFitness, Species Count = ${species.size}")
        species.filter { it.members.isNotEmpty()}.forEachIndexed { index, s ->
            val speciesMaxFitness = s.members.maxOfOrNull { it.fitness ?: 0.0 }
            val speciesMinFitness = s.members.minOfOrNull { it.fitness ?: Double.MAX_VALUE }
            println("Species $index: Best Fitness = $speciesMaxFitness, Worst Fitness = $speciesMinFitness")
        }
        // Add your condition to check for an acceptable solution and break if found
        if (maxFitness != null && maxFitness > 0.95) { // Break condition for high fitness
            println("High fitness achieved. Stopping...")
            break
        }
    }
}

fun irisPopulationGenerator(weightRange: ClosedRange<Double>, random: Random): InitialPopulationGenerator {
    return SimpleInitialPopulationGenerator(
        inputNodeCount = 4, // Adjusted for Iris input features
        outputNodeCount = 3, // Adjusted for Iris classes
        hiddenNodeCount = 0, // Added hidden nodes for complexity
        connectionDensity = 1.0,
        activationFunctions = listOf(ActivationFunction.SIGMOID), // Added RELU for variety
        random = random,
        randomWeight = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive)
    )
}

