package main

import algorithm.*
import algorithm.activation.SingleActivationFunctionSelection
import algorithm.crossover.RandomCrossover
import algorithm.evolve.*
import algorithm.weight.GaussianRandomWeight
import genome.ActivationFunction
import genome.NetworkGenome
import kotlin.random.Random

fun main() {
    val random = Random.Default
    val weightRange = -30.0..30.0
    // Step 3: Initialize components
    val initialPopulationGenerator = xorPopulationGenerator(weightRange, random)
    val fitnessEvaluator = XorFitnessEvaluator()
    val crossMutation = RandomCrossover(random)
    val geneticOperators =
            createDefaultGeneticOperators(
                    weightRange,
                    listOf(ActivationFunction.SIGMOID),
                    random,
                    InnovationTracker(),
                    InnovationTracker(),
                    SingleActivationFunctionSelection(ActivationFunction.SIGMOID)
            )
    val genomeMutator = DefaultGenomeMutator(createMutationOperations(geneticOperators), random)
    val compatabilityThreshold = 3.0
    val genomeCompatibility = GenomeCompatibilityTraditional(createDefaultCoefficients())
    val speciation = SpeciationImpl(compatabilityThreshold, genomeCompatibility, random)
    val fitnessSharing = FitnessSharingExponential()
    val populationSize = 100
    val crossMutateChance = 0.2

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
                    0.1
            )

    // Step 5: Setup and run the NEAT algorithm
    var currentPopulation = emptyList<NetworkGenome>()
    val maxGenerations = 1000
    for (generation in 1..maxGenerations) {
        print("Generation $generation: ")
        currentPopulation = neatProcess.executeGeneration(currentPopulation)
        // Step 6: Logging and observation
        val species = speciation.speciesList
        val maxFitness = currentPopulation.maxOfOrNull { it.fitness ?: 0.0 }
        val minFitness = currentPopulation.minOfOrNull { it.fitness ?: Double.MAX_VALUE }
        println(
                "Generation $generation: Max Fitness = $maxFitness, Species Count = ${species.size}"
        )
        species.filter { it.members.isNotEmpty() }.forEachIndexed { index, s ->
            val speciesMaxFitness = s.members.maxOfOrNull { it.fitness ?: 0.0 }
            val speciesMinFitness = s.members.minOfOrNull { it.fitness ?: Double.MAX_VALUE }
            println(
                    "Species $index: Best Fitness = $speciesMaxFitness, Worst Fitness = $speciesMinFitness"
            )
        }
        // Add your condition to check for an acceptable solution and break if found
    }
    // Summary of each species
    
}

fun xorPopulationGenerator(
        weightRange: ClosedRange<Double>,
        random: Random
): InitialPopulationGenerator {
    return SimpleInitialPopulationGenerator(
            inputNodeCount = 2,
            outputNodeCount = 1,
            hiddenNodeCount = 0,
            connectionDensity = 1.0,
            activationFunctions = listOf(ActivationFunction.SIGMOID),
            random = random,
            randomWeight =
                    GaussianRandomWeight(
                            random,
                            0.0,
                            1.0,
                            weightRange.start,
                            weightRange.endInclusive
                    )
    )
}
