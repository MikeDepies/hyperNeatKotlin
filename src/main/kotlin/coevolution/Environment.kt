package coevolution


import environment.*
import algorithm.network.NetworkProcessorFactory
import genome.NetworkGenome
import algorithm.GenomeMutator
import algorithm.fitnessevaluator.SensorInputGenerator
import kotlin.random.Random

interface Environment<E, A> {
    fun mutate(): Environment<E, A>
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
}
class MazeEnvironmentAdapter(
    private val mazeGenomeMutator: MazeGenomeMutator,
    private val networkProcessorFactory: NetworkProcessorFactory,
    private val mazeGenome: MazeGenome,
    override val resourceUsageLimit: Int,
    override var resourceUsageCount: Int = 0
) : Environment<MazeGenome, NetworkGenome> {
    override fun getModel(): MazeGenome = mazeGenome
    override fun mutate(): Environment<MazeGenome, NetworkGenome> {
        // Generate a mutated genome
        val mutatedGenome = mazeGenomeMutator.mutate(mazeGenome)

        // Return a new instance of MazeEnvironmentAdapter with the mutated genome
        return MazeEnvironmentAdapter(
            mazeGenomeMutator,
            networkProcessorFactory,
            MazeGenome(mutatedGenome.networkGenome, mutatedGenome.mazeThresholds, mutatedGenome.width, mutatedGenome.height),
            resourceUsageLimit
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
        // Create a new NetworkProcessor from the genome
        val networkProcessor = networkProcessorFactory.createProcessor(mazeGenome.networkGenome)

        // Use the NetworkProcessor to create a CPPNMazeQuery
        val cppnMazeQuery = CPPNMazeQuery(networkProcessor)

        // Generate a new maze environment using the CPPNMazeQuery
        val cppnMazeGenerator = CPPNMazeGenerator(mazeGenome.mazeThresholds, mazeGenome.width, mazeGenome.height)
        val mazeEnvironment = cppnMazeGenerator.generateMaze(cppnMazeQuery)
        if (mazeEnvironment == null) {
            return false
        }
        val tmazeEnvironment = TmazeEnvironment(mazeEnvironment)
        if (shortestPathToGoal(tmazeEnvironment) < 5) {
            return false
        }
        val mazeSolverTester = MazeSolverTester(networkProcessorFactory, SensorInputGenerator(tmazeEnvironment), tmazeEnvironment, 100)
        
        val solvedAgents = agents.count { agent -> mazeSolverTester.canSolveMaze(agent.getModel()) }
        val unsolvedAgents = agents.size - solvedAgents
        
        return unsolvedAgents > 1 //&& solvedAgents > 1
    }
}

