package coevolution


import environment.*
import algorithm.network.NetworkProcessorFactory
import genome.NetworkGenome
import algorithm.GenomeMutator
import algorithm.fitnessevaluator.SensorInputGenerator
import kotlin.random.Random

interface Environment<E, A> {
    fun mutate(potentialMates: List<Environment<E, A>>): Environment<E, A>
    val resourceUsageLimit: Int
    var resourceUsageCount: Int
    fun isResourceAvailable(): Boolean
    
    fun testAgents(agents: List<Agent<A, E>>): Boolean
    fun getModel(): E
}


data class MazeGenome(
    val networkGenome: NetworkGenome,
    val mazeThresholds: MazeThresholds,
    val width: Int,
    val height: Int
)

interface MazeGenomeMutator {
    fun mutate(mazeGenome: MazeGenome): MazeGenome
    fun crossMutate(mazeGenome1: MazeGenome, mazeGenome2: MazeGenome): MazeGenome
    fun rollCrossMutation(): Boolean
}
fun createMutationParametersWithAdjustedRange(
    wallThresholdRange: Double,
    goalPositionThresholdRange: Double,
    agentStartThresholdRange: Double,
    widthRange: Double,
    heightRange: Double
): MutationParameters {
    val calculateAdjustedRange = { range: Double -> Pair(1.0 - (range * 0.5), 1.0 + (range * 0.5)) }

    return MutationParameters(
        wallThresholdRange = calculateAdjustedRange(wallThresholdRange),
        goalPositionThresholdRange = calculateAdjustedRange(goalPositionThresholdRange),
        agentStartThresholdRange = calculateAdjustedRange(agentStartThresholdRange),
        widthRange = calculateAdjustedRange(widthRange),
        heightRange = calculateAdjustedRange(heightRange)
    )
}



data class MutationParameters(
    val wallThresholdRange: Pair<Double, Double>,
    val goalPositionThresholdRange: Pair<Double, Double>,
    val agentStartThresholdRange: Pair<Double, Double>,
    val widthRange: Pair<Double, Double>,
    val heightRange: Pair<Double, Double>
)

class SimpleMazeGenomeMutator(
    private val genomeMutator: GenomeMutator,
    private val random: Random,
    private val mutationParameters: MutationParameters
) : MazeGenomeMutator {
    private fun randomPerturbation(min: Double, max: Double): Double = min + (random.nextDouble() * (max - min))

    override fun mutate(mazeGenome: MazeGenome): MazeGenome {
        val mutatedNetworkGenome = genomeMutator.mutateGenome(mazeGenome.networkGenome)

        val mutatedMazeThresholds = mazeGenome.mazeThresholds.copy(
            wallThreshold = mazeGenome.mazeThresholds.wallThreshold * randomPerturbation(
                mutationParameters.wallThresholdRange.first, mutationParameters.wallThresholdRange.second
            ),
            goalPositionThreshold = mazeGenome.mazeThresholds.goalPositionThreshold * randomPerturbation(
                mutationParameters.goalPositionThresholdRange.first, mutationParameters.goalPositionThresholdRange.second
            ),
            agentStartThreshold = mazeGenome.mazeThresholds.agentStartThreshold * randomPerturbation(
                mutationParameters.agentStartThresholdRange.first, mutationParameters.agentStartThresholdRange.second
            )
        )
        val mutatedWidth = (mazeGenome.width * randomPerturbation(
            mutationParameters.widthRange.first, mutationParameters.widthRange.second
        ) + .5).toInt()
        val mutatedHeight = (mazeGenome.height * randomPerturbation(
            mutationParameters.heightRange.first, mutationParameters.heightRange.second
        ) + .5).toInt()
        // println("Mutated width: $mutatedWidth, Mutated height: $mutatedHeight")
        return mazeGenome.copy(
            networkGenome = mutatedNetworkGenome,
            mazeThresholds = mutatedMazeThresholds,
            width = mutatedWidth,
            height = mutatedHeight
        )
    }
    override fun crossMutate(mazeGenome1: MazeGenome, mazeGenome2: MazeGenome): MazeGenome {
        return MazeGenome(
            networkGenome = genomeMutator.crossMutateGenomes(mazeGenome1.networkGenome, mazeGenome2.networkGenome),
            mazeThresholds = mazeGenome1.mazeThresholds,
            width = mazeGenome1.width,
            height = mazeGenome1.height
        )
    }
    override fun rollCrossMutation(): Boolean {
        return genomeMutator.rollCrossMutation()
    }
}
class MazeEnvironmentAdapter(
    private val mazeAgentCache: MazeAgentCache,
    private val mazeEnvironmentCache: MazeEnvironmentCache,
    private val mazeGenomeMutator: MazeGenomeMutator,
    // private val networkProcessorFactory: NetworkProcessorFactory,
    private val mazeGenome: MazeGenome,
    override val resourceUsageLimit: Int,
    private val stepsAllowed: Int,
    override var resourceUsageCount: Int = 0,
) : Environment<MazeGenome, NetworkGenome> {
    override fun getModel(): MazeGenome = mazeGenome
    override fun mutate(potentialMates: List<Environment<MazeGenome, NetworkGenome>>): Environment<MazeGenome, NetworkGenome> {
        // Generate a mutated genome
        val mutatedGenome = if (mazeGenomeMutator.rollCrossMutation()) {
            mazeGenomeMutator.crossMutate(mazeGenome, potentialMates.random().getModel())
        } else {
            mazeGenomeMutator.mutate(mazeGenome)
        }

        // Return a new instance of MazeEnvironmentAdapter with the mutated genome
        return MazeEnvironmentAdapter(
            mazeAgentCache,
            mazeEnvironmentCache,
            mazeGenomeMutator,
            // networkProcessorFactory,
            MazeGenome(mutatedGenome.networkGenome, mutatedGenome.mazeThresholds, mutatedGenome.width, mutatedGenome.height),
            resourceUsageLimit,
            stepsAllowed
        )
    }

    override fun isResourceAvailable(): Boolean {
        // Example resource check: Ensure we haven't exceeded the usage limit
        return resourceUsageCount < resourceUsageLimit
    }

    fun canBeSolved(agent: Agent<NetworkGenome, MazeGenome>, mazeSolverTester: MazeSolverTester): Boolean {
        
        return mazeSolverTester.canSolveMaze(agent.getModel())
    }

    override fun testAgents(agents: List<Agent<NetworkGenome, MazeGenome>>): Boolean {
        val mazeEnvironment = mazeEnvironmentCache.getMazeEnvironment(mazeGenome)
        if (mazeEnvironment == null) {
            return false
        }
        val tmazeEnvironment = TmazeEnvironment(mazeEnvironment, mazeEnvironment.width, mazeEnvironment.height)
        // if (shortestPathToGoal(tmazeEnvironment) < 1) {
        //     return false
        // }
        val mazeSolverTester = MazeSolverTester(mazeAgentCache, SensorInputGenerator(tmazeEnvironment), tmazeEnvironment, stepsAllowed)
        
        val solvedAgents = agents.count { agent -> 
            tmazeEnvironment.reset()
            mazeSolverTester.canSolveMaze(agent.getModel()) }
        val unsolvedAgents = agents.size - solvedAgents
        
        return solvedAgents < 2 && solvedAgents > 0
    }
}

class MazeEnvironmentCache(
    private val networkProcessorFactory: NetworkProcessorFactory,
    private val mazeGenomeMutator: MazeGenomeMutator
) {
    private val cache: MutableMap<NetworkGenome, MazeEnvironment?> = mutableMapOf()

    fun getMazeEnvironment(mazeGenome: MazeGenome): MazeEnvironment? {
        return cache.getOrPut(mazeGenome.networkGenome) {
            val networkProcessor = networkProcessorFactory.createProcessor(mazeGenome.networkGenome)
            val cppnMazeQuery = CPPNMazeQuery(networkProcessor)
            val cppnMazeGenerator = CPPNMazeGenerator(mazeGenome.mazeThresholds, mazeGenome.width, mazeGenome.height)
            cppnMazeGenerator.generateMaze(cppnMazeQuery)
        }
    }
    fun clearCache() {
        cache.clear()
    }
}

