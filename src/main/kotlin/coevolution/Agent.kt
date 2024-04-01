package coevolution

import algorithm.GenomeMutator
import algorithm.network.NetworkBuilder
import algorithm.network.NetworkProcessorFactory
import algorithm.fitnessevaluator.SensorInputGenerator
import algorithm.fitnessevaluator.EnhancedStateEncoderDecoder
import environment.*
import genome.NetworkGenome

interface Agent<M, E> {
    fun mutate(): Agent<M, E>
    fun satisfiesMinimalCriterion(environment: Environment<E, M>): Boolean
}

class MazeSolverAgent(
    private val genome: NetworkGenome,
    private val genomeMutator: GenomeMutator,
    val networkProcessorFactory: NetworkProcessorFactory
) : Agent<NetworkGenome, MazeGenome> {
    override fun mutate(): Agent<NetworkGenome, MazeGenome> {
        val mutatedGenome = genomeMutator.mutateGenome(genome)
        return MazeSolverAgent(mutatedGenome, genomeMutator, networkProcessorFactory)
    }

    override fun satisfiesMinimalCriterion(environment: Environment<MazeGenome, NetworkGenome>): Boolean {
        if (environment !is MazeEnvironmentAdapter) return false
        val mazeEnvironment = environment.getModel()

        val networkProcessor = networkProcessorFactory.createProcessor(genome)
        val mazeQuery = CPPNMazeQuery(networkProcessor)
        val mazeGenerator = CPPNMazeGenerator(mazeEnvironment.mazeThresholds, mazeEnvironment.width, mazeEnvironment.height)
        val generatedMaze = mazeGenerator.generateMaze(mazeQuery)
        return generatedMaze?.let { maze ->
            val tmazeEnvironment = TmazeEnvironment(maze)
            MazeSolverTester(networkProcessorFactory, SensorInputGenerator(tmazeEnvironment), tmazeEnvironment).canSolveMaze(genome)
        } ?: false
    }
}


class MazeSolverTester(
    private val networkProcessorFactory: NetworkProcessorFactory,
    private val sensorInputGenerator: SensorInputGenerator,
    private val environment: TmazeEnvironment,
    private val stepsAllowed: Int = 100 // Default steps allowed to solve the maze
) {

    fun canSolveMaze(genome: NetworkGenome): Boolean {
        environment.reset()
        val mazeBoundaries = deriveMazeBoundaries(environment.environment)
        val enhancedStateEncoderDecoder =
            EnhancedStateEncoderDecoder(mazeBoundaries, sensorInputGenerator)
        val networkProcessor =
            networkProcessorFactory.createProcessor(genome)

        for (step in 0 until stepsAllowed) {
            val inputs = enhancedStateEncoderDecoder.encodeAgentState(
                environment.agentPosition,
                environment.environment.rewardSide
            )
            val output = networkProcessor.feedforward(inputs)
            val action = enhancedStateEncoderDecoder.decodeAction(output)

            val (reachedGoal, _) = environment.step(action)
            if (reachedGoal) return true // Successfully solved the maze
        }

        return false // Failed to solve the maze within the allotted steps
    }
}