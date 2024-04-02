package main

import algorithm.*
import algorithm.fitnessevaluator.IrisFitnessEvaluator
import algorithm.activation.SingleActivationFunctionSelection
import algorithm.createMutationOperations
import algorithm.crossover.RandomCrossover
import algorithm.crossover.BiasedCrossover
import genome.ActivationFunction
import genome.NetworkGenome
import algorithm.evolve.*
import algorithm.weight.GaussianRandomWeight
import kotlin.random.Random

private fun createCoefficients() = Coefficients(1.0, 1.0, 0.4)

private fun createMutationOperations(geneticOperators: GeneticOperators, random: Random): GenomeMutatorConfig {
    return fromList(listOf(
            MutationOperation(0.04, geneticOperators.mutateAddConnection),
            MutationOperation(0.01, geneticOperators.mutateAddNode),
            MutationOperation(0.9, geneticOperators.mutateWeights),
            MutationOperation(0.04, geneticOperators.mutateActivationFunction),
            MutationOperation(0.05, geneticOperators.mutateConnectionEnabled)
    ), CrossOverOperation(geneticOperators.crossMutation), random)
}
fun main() {
    val random = Random(0)
    val weightRange = -3.0..3.0
    // Step 3: Initialize components
    val nodeInnovationTracker = InnovationTracker()
    val weight = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive)
    val weightMutationConfig = WeightMutationConfig(weight, .7, (-.001..0.001))
    val connectionInnovationTracker = InnovationTracker()
    val initialPopulationGenerator = irisPopulationGenerator(weightRange, random, nodeInnovationTracker, connectionInnovationTracker)
    val fitnessEvaluator = IrisFitnessEvaluator(random)
    val crossMutation = RandomCrossover(random)
    val geneticOperators = createDefaultGeneticOperators(
        crossMutation,
        listOf(ActivationFunction.SIGMOID),
        random,
        nodeInnovationTracker,
        connectionInnovationTracker,
        SingleActivationFunctionSelection(ActivationFunction.SIGMOID),
        weightMutationConfig
    )
    val genomeMutator = DefaultGenomeMutator(
        createMutationOperations(geneticOperators, random)
    )
    val compatabilityThreshold = 4.0
    val genomeCompatibility = GenomeCompatibilityTraditional(createCoefficients())
    val speciation = SpeciationImpl(compatabilityThreshold, genomeCompatibility, random)
    val fitnessSharing = FitnessSharingExponential()
    val maxGenerations = 5000 // Adjusted for complexity of Iris problem
    val populationSize = 100 // Adjusted for Iris dataset size
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
        0.5
    )

    // Step 5: Setup and run the NEAT algorithm
    var currentPopulation = emptyList<NetworkGenome>()
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
//        if (maxFitness != null && maxFitness > 0.95) { // Break condition for high fitness
//            println("High fitness achieved. Stopping...")
//            break
//        }
    }
}

fun irisPopulationGenerator(weightRange: ClosedRange<Double>, random: Random, nodeInnovationTracker: InnovationTracker, connectionInnovationTracker: InnovationTracker): InitialPopulationGenerator {
    return SimpleInitialPopulationGenerator(
        inputNodeCount = 4, // Adjusted for Iris input features
        outputNodeCount = 3, // Adjusted for Iris classes
        hiddenNodeCount = 0, // Added hidden nodes for complexity
        connectionDensity = 1.0,
        activationFunctions = listOf(ActivationFunction.SIGMOID), // Added RELU for variety
        random = random,
        randomWeight = GaussianRandomWeight(random, 0.0, 1.0, weightRange.start, weightRange.endInclusive),
        nodeInnovationTracker = nodeInnovationTracker,
        connectionInnovationTracker = connectionInnovationTracker
    )
}

