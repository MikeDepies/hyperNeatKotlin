package algorithm.evolve

import genome.NetworkGenome
import population.Species
import algorithm.FitnessEvaluator
import algorithm.crossover.CrossMutation
import algorithm.GenomeMutator
import kotlin.random.Random

interface NEATProcess {
    /**
     * Categorizes the given genomes into species based on their compatibility.
     * @param genomes The list of genomes to be categorized.
     */
    fun categorizeIntoSpecies(genomes: List<NetworkGenome>)

    /**
     * Applies fitness sharing within each species to promote diversity.
     * @param speciesList The list of species for which to share fitness.
     */
    fun shareFitnessWithinSpecies(speciesList: List<Species>)

    /**
     * Selects genomes for reproduction based on their fitness and possibly other criteria.
     * @param speciesList The list of species from which to select genomes.
     * @return A list of selected genomes for reproduction.
     */
    fun selectForReproduction(speciesList: List<Species>): List<NetworkGenome>

    /**
     * Generates offspring from the selected genomes through crossover and mutation.
     * @param selectedGenomes The genomes selected for reproduction.
     * @return A list of offspring genomes.
     */
    fun reproduce(selectedGenomes: List<NetworkGenome>): List<NetworkGenome>

    /**
     * Replaces the least fit genomes in the population with new offspring, maintaining population size.
     * @param currentPopulation The current population of genomes.
     * @param offspring The offspring to be introduced into the population.
     * @return The new population of genomes.
     */
    fun replaceLeastFit(currentPopulation: List<NetworkGenome>, offspring: List<NetworkGenome>): List<NetworkGenome>

    /**
     * Executes one generation of the NEAT process, including species categorization, fitness sharing,
     * selection, reproduction, and population replacement.
     * @param currentPopulation The current population of genomes.
     * @return The new population of genomes after one generation.
     */
    fun executeGeneration(currentPopulation: List<NetworkGenome>): List<NetworkGenome>
}

class NEATProcessImpl(
    private val initialPopulationGenerator: InitialPopulationGenerator,
    private val fitnessEvaluator: FitnessEvaluator,
    private val crossMutation: CrossMutation,
    private val genomeMutator: GenomeMutator,
    private val speciation: Speciation,
    private val fitnessSharing: FitnessSharing,
    private val populationSize: Int,
    private val crossMutateChance: Double,
    private val random: Random,
    private val survivalRate: Double
) : NEATProcess {

    override fun categorizeIntoSpecies(genomes: List<NetworkGenome>) {
        speciation.categorizeIntoSpecies(genomes)
    }

    override fun shareFitnessWithinSpecies(speciesList: List<Species>) {
        speciesList.forEach { species ->
            fitnessSharing.shareFitnessWithinSpecies(species)
        }
    }

    override fun selectForReproduction(speciesList: List<Species>): List<NetworkGenome> {
        return speciesList.flatMap { species ->
            species.members.sortedByDescending { it.sharedFitness }.take((species.members.size * survivalRate).toInt())
        }
    }

    override fun reproduce(selectedGenomes: List<NetworkGenome>): List<NetworkGenome> {
        val offspring = mutableListOf<NetworkGenome>()
        
        // Assuming `selectedGenomes` are already grouped by species
        val speciesGroups = selectedGenomes.groupBy { it.speciesId }

        speciesGroups.values.forEach { group ->
            val shuffledGenomes = group.shuffled()

            // Calculate the number of offspring this species should produce
            val offspringCount = calculateOffspringCount(group, populationSize)

            for (index in 0 until offspringCount) {
                val parent1Index = random.nextInt(shuffledGenomes.size)
                var parent2Index = random.nextInt(shuffledGenomes.size)
                // Ensure different parents are selected for crossover
                while (parent1Index == parent2Index) {
                    parent2Index = random.nextInt(shuffledGenomes.size)
                }
                val parent1 = shuffledGenomes[parent1Index]
                val parent2 = shuffledGenomes[parent2Index]

                when (random.nextDouble()) {
                    in 0.0..crossMutateChance -> {
                        // Perform sexual reproduction with crossover
                        offspring.add(crossMutation.crossover(parent1, parent2))
                    }
                    else -> {
                        // Perform asexual reproduction with mutation
                        offspring.add(genomeMutator.mutateGenome(parent1))
                    }
                }
            }
        }
        return offspring
    }

    private fun calculateOffspringCount(speciesGroup: List<NetworkGenome>, totalPopulationSize: Int): Int {
        // Example calculation based on species size proportion of total population
        // This can be adjusted based on fitness or other criteria
        val speciesProportion = speciesGroup.size.toFloat() / totalPopulationSize
        return (speciesProportion * totalPopulationSize).toInt()
    }

    override fun replaceLeastFit(currentPopulation: List<NetworkGenome>, offspring: List<NetworkGenome>): List<NetworkGenome> {
        val sortedPopulation = currentPopulation.sortedBy { it.fitness }
        return sortedPopulation.takeLast(sortedPopulation.size - offspring.size) + offspring
    }

    override fun executeGeneration(currentPopulation: List<NetworkGenome>): List<NetworkGenome> {
        val genomes = if (currentPopulation.isEmpty()) {
            List(populationSize) { initialPopulationGenerator.generateSingleGenome() }
        } else {
            currentPopulation
        }
        println("population size: ${genomes.size}")

        genomes.forEach { it.fitness = fitnessEvaluator.calculateFitness(it) }
        println("Fitness calculated")
        categorizeIntoSpecies(genomes)
        println("Categorized into species")
        shareFitnessWithinSpecies(speciation.speciesList)
        println("Shared fitness within species")
        val selectedForReproduction = selectForReproduction(speciation.speciesList)
        println("Selected for reproduction")
        println("Selected for reproduction: ${selectedForReproduction.size}")
        val offspring = reproduce(selectedForReproduction)
        return replaceLeastFit(genomes, offspring)
    }
}

class NEATProcessWithDirectReplacement(
    private val initialPopulationGenerator: InitialPopulationGenerator,
    private val fitnessEvaluator: FitnessEvaluator,
    private val crossMutation: CrossMutation,
    private val genomeMutator: GenomeMutator,
    private val speciation: Speciation,
    private val fitnessSharing: FitnessSharing,
    private val populationSize: Int,
    private val crossMutateChance: Double,
    private val random: Random,
    private val survivalRate: Double
) : NEATProcess {

    override fun categorizeIntoSpecies(genomes: List<NetworkGenome>) {
        speciation.categorizeIntoSpecies(genomes)
    }

    override fun shareFitnessWithinSpecies(speciesList: List<Species>) {
        speciesList.forEach { species ->
            fitnessSharing.shareFitnessWithinSpecies(species)
        }
    }

    override fun selectForReproduction(speciesList: List<Species>): List<NetworkGenome> {
        return speciesList.flatMap { species ->
            species.members.sortedByDescending { it.sharedFitness }.take((species.members.size * survivalRate).toInt())
        }
    }

    override fun reproduce(selectedGenomes: List<NetworkGenome>): List<NetworkGenome> {
        val offspring = mutableListOf<NetworkGenome>()
        val totalFitness = selectedGenomes.sumOf { it.fitness ?: 0.0 }
        val speciesGroups = selectedGenomes.groupBy { it.speciesId }
        var offspringCountSoFar = 0

        // Calculate offspring count for each species and produce offspring
        val offspringCounts = speciesGroups.mapValues { (_, group) ->
            val groupFitness = group.sumOf { it.fitness ?: 0.0 }
            val offspringCount = ((groupFitness / totalFitness) * populationSize).toInt()
            offspringCount
        }

        speciesGroups.forEach { (_, group) ->
            val groupOffspringCount = offspringCounts[group.first().speciesId] ?: 0
            offspringCountSoFar += groupOffspringCount

            for (i in 0 until groupOffspringCount) {
                
                val child = when {
                    (group.size > 1) && (random.nextDouble() <= crossMutateChance) -> {
                        val parents = group.randomPair()
                        crossMutation.crossover(parents.first, parents.second)
                    }
                    else -> {
                        genomeMutator.mutateGenome(group.random(random))
                    }
                }
                offspring.add(child)
            }
        }

        // Adjust the total offspring count to match the population size exactly
        adjustOffspringCount(offspring, offspringCountSoFar, selectedGenomes)

        return offspring
    }

    private fun adjustOffspringCount(offspring: MutableList<NetworkGenome>, offspringCountSoFar: Int, selectedGenomes: List<NetworkGenome>) {
        val adjustment = populationSize - offspringCountSoFar
        if (adjustment > 0) {
            // Need to add more offspring
            repeat(adjustment) {
                val parent = selectedGenomes.random(random)
                val child = when (random.nextDouble()) {
                    in 0.0..crossMutateChance -> genomeMutator.mutateGenome(parent)
                    else -> parent // In case mutation is not chosen, just clone the parent
                }
                offspring.add(child)
            }
        } else if (adjustment < 0) {
            // Need to remove some offspring
            repeat(-adjustment) {
                if (offspring.isNotEmpty()) offspring.removeAt(random.nextInt(offspring.size))
            }
        }
    }

    override fun replaceLeastFit(currentPopulation: List<NetworkGenome>, offspring: List<NetworkGenome>): List<NetworkGenome> {
        // Direct replacement: Replace the entire population with offspring, assuming the sizes match.
        return offspring
    }

    override fun executeGeneration(currentPopulation: List<NetworkGenome>): List<NetworkGenome> {
        val genomes = if (currentPopulation.isEmpty()) {
            List(populationSize) { initialPopulationGenerator.generateSingleGenome() }
        } else {
            currentPopulation
        }
        
        genomes.forEach { it.fitness = fitnessEvaluator.calculateFitness(it) }
        categorizeIntoSpecies(genomes)
        shareFitnessWithinSpecies(speciation.speciesList)
        val selectedForReproduction = selectForReproduction(speciation.speciesList)
        val offspring = reproduce(selectedForReproduction)
        return replaceLeastFit(genomes, offspring)
    }

    private fun List<NetworkGenome>.randomPair(): Pair<NetworkGenome, NetworkGenome> {
        val firstParent = this.random(random)
        var secondParent = this.random(random)
        // while (firstParent === secondParent) {
        //     println("(${this.size}) Same parent selected. Selecting another parent...")
        //     secondParent = this.random(random)
        // }
        return firstParent to secondParent
    }

    private fun List<NetworkGenome>.weightedRandom(weight: Double): NetworkGenome {
        var cumulativeWeight = 0.0
        this.forEach { genome ->
            cumulativeWeight += genome.fitness ?: 0.0
            if (cumulativeWeight >= weight) return genome
        }
        return this.last()
    }
}