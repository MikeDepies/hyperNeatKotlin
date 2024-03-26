package algorithm.fitnessevaluator

import algorithm.FitnessEvaluator
import algorithm.network.DefaultActivationFunctionMapper
import algorithm.network.NetworkBuilder
import algorithm.network.NetworkProcessorFactory
import environment.*
import genome.NetworkGenome

class TmazeFitnessEvaluator(val rewardSide: RewardSide) : FitnessEvaluator {
    val environment = TmazeEnvironment(createTMaze(rewardSide))

    override fun calculateFitness(genome: NetworkGenome): Double {
        var fitness = 1.0
        val stepsAllowed = 10 // Maximum steps allowed to find the reward
        environment.reset()

        val networkProcessor = NetworkProcessorFactory(NetworkBuilder(DefaultActivationFunctionMapper())).createProcessor(genome)

        for (step in 0 until stepsAllowed) {
            val inputs = StateEncoderDecoder.encodeAgentState(environment.agentPosition, environment.environment.rewardSide)
            val output = networkProcessor.feedforward(inputs)
            val action = StateEncoderDecoder.decodeAction(output)

            val (reachedGoal, reward) = environment.step(action)
            fitness += reward
            if (reachedGoal) break
        }

        return fitness
    }
}

class StateEncoderDecoder {
    companion object {
        fun encodeAgentState(agentPosition: Position, rewardSide: RewardSide): List<Double> {
            // Encoding the agent's position and the reward side into a list of doubles
            // Assuming the maze's dimensions are known and fixed, e.g., 5x5
            val encodedPosition = listOf(agentPosition.x.toDouble() / 5.0, agentPosition.y.toDouble() / 5.0)
            val encodedRewardSide = when (rewardSide) {
                RewardSide.LEFT -> 0.0
                RewardSide.RIGHT -> 1.0
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
}
