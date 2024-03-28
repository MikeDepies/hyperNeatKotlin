package main

import algorithm.*
import algorithm.activation.SingleActivationFunctionSelection
import algorithm.crossover.BiasedCrossover
import algorithm.evolve.*
import algorithm.fitnessevaluator.XorFitnessEvaluator
import algorithm.weight.GaussianRandomWeight
import genome.ActivationFunction
import genome.NetworkGenome
import kotlin.random.Random

private fun createXorCoefficients() = Coefficients(1.0, 1.0, 0.4)

private fun createMutationOperations(geneticOperators: GeneticOperators): List<MutationOperation> {
    return listOf(
        MutationOperation(.04, geneticOperators.mutateAddConnection),
        MutationOperation(.01, geneticOperators.mutateAddNode),
        MutationOperation(0.9, geneticOperators.mutateWeights),
        // MutationOperation(0.04, geneticOperators.mutateActivationFunction),
        MutationOperation(0.05, geneticOperators.mutateConnectionEnabled)
    )
}

fun main() {
    val random = Random(1)
    val weightRange = -3.0..3.0
    // Step 3: Initialize components
    val weight =
        GaussianRandomWeight(
            random,
            0.0,
            1.0,
            weightRange.start,
            weightRange.endInclusive
        )
    val weightMutationConfig = WeightMutationConfig(weight, .7, (-.1..0.1))
    val nodeInnovationTracker = InnovationTracker()
    val connectionInnovationTracker = InnovationTracker()
    val initialPopulationGenerator =
        xorPopulationGenerator(
            weightRange,
            random,
            nodeInnovationTracker,
            connectionInnovationTracker
        )
    val fitnessEvaluator = XorFitnessEvaluator()
    val crossMutation = BiasedCrossover(random)
    val geneticOperators =
        createDefaultGeneticOperators(
            weightRange,
            listOf(ActivationFunction.SIGMOID),
            random,
            nodeInnovationTracker,
            connectionInnovationTracker,
            SingleActivationFunctionSelection(
                ActivationFunction.SIGMOID
            ),
            weightMutationConfig
        )

    val genomeMutator = DefaultGenomeMutator(createMutationOperations(geneticOperators), random)
    val compatabilityThreshold = 10.0
    val genomeCompatibility = GenomeCompatibilityTraditional(createXorCoefficients())
    val speciation = SpeciationImpl(compatabilityThreshold, genomeCompatibility, random)
    val fitnessSharing = FitnessSharingExponential()
    val populationSize = 500
    val crossMutateChance = 0.0

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
        var count = 0
        currentPopulation.forEach { genome ->
            val connectionIds = genome.connectionGenes.map { it.id }
            val uniqueIds = connectionIds.toSet()
            if (connectionIds.size != uniqueIds.size) {
                count++
                println("Genome ${genome} has overlapping connection IDs.")
            }
        }
        if (count > 0) {
            println("Found $count genomes with overlapping connection IDs.")
            System.exit(1)
        }
        // Step 6: Logging and observation
        val species = speciation.speciesList
        val maxFitness = currentPopulation.maxOfOrNull { it.fitness ?: 0.0 }
        val minFitness = currentPopulation.minOfOrNull { it.fitness ?: Double.MAX_VALUE }
        println(
                "Generation $generation: Max Fitness (${currentPopulation.count{ it.fitness == null}}) = $maxFitness, Species Count = ${species.size}"
        )
        species.filter { it.members.isNotEmpty() }.forEachIndexed { index, s ->
            val speciesMaxFitness = s.members.maxOfOrNull { it.fitness ?: 0.0 }
            val speciesMinFitness =
                s.members.minOfOrNull { it.fitness ?: Double.MAX_VALUE }
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
    random: Random,
    nodeInnovationTracker: InnovationTracker,
    connectionInnovationTracker: InnovationTracker,
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
        ),
        nodeInnovationTracker,
        connectionInnovationTracker
    )
}
