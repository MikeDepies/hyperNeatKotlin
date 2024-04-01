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
    fun satisfiesMinimalCriterion(agent: Agent<A, E>): Boolean
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
    val calculateAdjustedRange = { range: Double -> Pair(1.0 - (range * 0.5), range) }

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
        )).toInt()
        val mutatedHeight = (mazeGenome.height * randomPerturbation(
            mutationParameters.heightRange.first, mutationParameters.heightRange.second
        )).toInt()

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
            MazeGenome(mutatedGenome.networkGenome, mazeGenome.mazeThresholds, mazeGenome.width, mazeGenome.height),
            resourceUsageLimit
        )
    }

    override fun isResourceAvailable(): Boolean {
        // Example resource check: Ensure we haven't exceeded the usage limit
        return resourceUsageCount < resourceUsageLimit
    }

    override fun satisfiesMinimalCriterion(agent: Agent<NetworkGenome, MazeGenome>): Boolean {
        // Create a new NetworkProcessor from the genome
        val networkProcessor = networkProcessorFactory.createProcessor(mazeGenome.networkGenome)

        // Use the NetworkProcessor to create a CPPNMazeQuery
        val cppnMazeQuery = CPPNMazeQuery(networkProcessor)

        // Generate a new maze environment using the CPPNMazeQuery
        val cppnMazeGenerator = CPPNMazeGenerator(mazeGenome.mazeThresholds, mazeGenome.width, mazeGenome.height)
        val mazeEnvironment = cppnMazeGenerator.generateMaze(cppnMazeQuery)

        // Check if the generated maze environment satisfies the minimal criterion
        // For example, ensuring there's a path from the agent to the goal
        return mazeEnvironment?.let { env ->
            val tmazeEnvironment = TmazeEnvironment(env)
            shortestPathToGoal(tmazeEnvironment) > 2 && !MazeSolverTester(networkProcessorFactory, SensorInputGenerator(tmazeEnvironment), tmazeEnvironment).canSolveMaze(agent.getModel())
        } ?: false
    }
}