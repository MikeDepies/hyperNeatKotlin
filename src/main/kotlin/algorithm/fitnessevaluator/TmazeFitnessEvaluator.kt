package algorithm.fitnessevaluator

import algorithm.FitnessEvaluator
import algorithm.network.DefaultActivationFunctionMapper
import algorithm.network.NetworkBuilder
import algorithm.network.NetworkProcessorFactory
import environment.*
import genome.NetworkGenome
import kotlin.random.Random

class TmazeFitnessEvaluator(val environment: TmazeEnvironment) : FitnessEvaluator {

    

    override fun calculateFitness(genome: NetworkGenome): Double {
        var fitness = 1.0
        val stepsAllowed = 100 // Maximum steps allowed to find the reward
        environment.reset()
        val mazeBoundaries = deriveMazeBoundaries(environment.environment)
        val stateEncoderDecoder = StateEncoderDecoder(mazeBoundaries)
        val networkProcessor =
                NetworkProcessorFactory(NetworkBuilder(DefaultActivationFunctionMapper()))
                        .createProcessor(genome)

        for (step in 0 until stepsAllowed) {
            val inputs =
                    stateEncoderDecoder.encodeAgentState(
                            environment.agentPosition,
                            environment.environment.rewardSide
                    )
            val output = networkProcessor.feedforward(inputs)
            val action = stateEncoderDecoder.decodeAction(output)

            val (reachedGoal, reward) = environment.step(action)
            fitness += reward
            if (reachedGoal) break
        }

        return fitness
    }
}

class MazeFitnessEvaluatorSensor(
        val sensorInputGenerator: SensorInputGenerator,
        val environment: TmazeEnvironment
) : FitnessEvaluator {

    override fun calculateFitness(genome: NetworkGenome): Double {
        var fitness = 0.0
        val stepsAllowed = 100 // Maximum steps allowed to find the reward
        environment.reset()
        val mazeBoundaries = deriveMazeBoundaries(environment.environment)
        val enhancedStateEncoderDecoder =
                EnhancedStateEncoderDecoder(mazeBoundaries, sensorInputGenerator)
        val networkProcessor =
                NetworkProcessorFactory(NetworkBuilder(DefaultActivationFunctionMapper()))
                        .createProcessor(genome)

        for (step in 0 until stepsAllowed) {
            val inputs =
                    enhancedStateEncoderDecoder.encodeAgentState(
                            environment.agentPosition,
                            environment.environment.rewardSide
                    )
            val output = networkProcessor.feedforward(inputs)
            val action = enhancedStateEncoderDecoder.decodeAction(output)

            val (reachedGoal, reward) = environment.step(action)
            fitness += reward
            if (reachedGoal) break
        }

        return fitness
    }
}

class SensorInputGenerator(val environment: TmazeEnvironment) {

    private val directions =
            listOf(
                    Pair(-1, 0), // Left
                    Pair(1, 0), // Right
                    Pair(0, -1), // Up
                    Pair(0, 1) // Down
            )

    fun generateSensorData(): List<Double> {
        return directions.map { (dx, dy) ->
            val checkPosition =
                    environment.agentPosition.copy(
                            x = environment.agentPosition.x + dx,
                            y = environment.agentPosition.y + dy
                    )
            if (checkPosition in environment.mazeStructure) 1.0 else 0.0
        }
    }
}

class EnhancedStateEncoderDecoder(
        private val mazeBoundaries: Pair<Position, Position>,
        private val sensorInputGenerator: SensorInputGenerator
) {

    fun encodeAgentState(agentPosition: Position, rewardSide: RewardSide): List<Double> {
        // Encoding the agent's position, the reward side, the maze boundaries, and sensor data into
        // a list of doubles
        val mazeWidth = mazeBoundaries.second.x.toDouble() - mazeBoundaries.first.x.toDouble() + 1.0
        val mazeHeight =
                mazeBoundaries.second.y.toDouble() - mazeBoundaries.first.y.toDouble() + 1.0
        val normalizedX = (agentPosition.x.toDouble() - mazeBoundaries.first.x) / mazeWidth
        val normalizedY = (agentPosition.y.toDouble() - mazeBoundaries.first.y) / mazeHeight
        val encodedPosition = listOf(normalizedX, normalizedY)
        val encodedRewardSide =
                when (rewardSide) {
                    RewardSide.LEFT -> 0.0
                    RewardSide.RIGHT -> 1.0
                    RewardSide.CENTER -> 0.5
                    RewardSide.RANDOM -> -1.0
                }
        val sensorData = sensorInputGenerator.generateSensorData()

        return encodedPosition + listOf(encodedRewardSide) + sensorData
    }

    fun decodeAction(output: List<Double>): Action {
        // Decoding the network's output into an action
        val maxIndex = output.indexOf(output.maxOrNull() ?: 0.0)
        return when (maxIndex) {
            0 -> Action.MOVE_FORWARD
            1 -> Action.MOVE_LEFT
            2 -> Action.MOVE_RIGHT
            else -> Action.MOVE_FORWARD // Default action
        }
    }
}

class StateEncoderDecoder(val mazeBoundaries: Pair<Position, Position>) {

    fun encodeAgentState(agentPosition: Position, rewardSide: RewardSide): List<Double> {
        // Encoding the agent's position, the reward side, and the maze boundaries into a list of
        // doubles
        val mazeWidth = mazeBoundaries.second.x.toDouble() - mazeBoundaries.first.x.toDouble() + 1.0
        val mazeHeight =
                mazeBoundaries.second.y.toDouble() - mazeBoundaries.first.y.toDouble() + 1.0
        val normalizedX = (agentPosition.x.toDouble() - mazeBoundaries.first.x) / mazeWidth
        val normalizedY = (agentPosition.y.toDouble() - mazeBoundaries.first.y) / mazeHeight
        val encodedPosition = listOf(normalizedX, normalizedY)
        val encodedRewardSide =
                when (rewardSide) {
                    RewardSide.LEFT -> 0.0
                    RewardSide.RIGHT -> 1.0
                    RewardSide.CENTER -> 0.5
                    RewardSide.RANDOM -> -1.0
                }
        return encodedPosition + encodedRewardSide
    }

    fun decodeAction(output: List<Double>): Action {
        // Decoding the network's output into an action
        // Assuming the output is a list of doubles representing probabilities for each action
        val maxIndex = output.indexOf(output.maxOrNull() ?: 0.0)
        return when (maxIndex) {
            0 -> Action.MOVE_FORWARD
            1 -> Action.MOVE_LEFT
            2 -> Action.MOVE_RIGHT
            else -> Action.MOVE_FORWARD // Default action
        }
    }
}
