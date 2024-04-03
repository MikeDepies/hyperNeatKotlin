package coevolution

import environment.renderEnvironmentAsString
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.stream.Collectors
import kotlin.math.max

data class SolvedEnvironment<E, A>(
    val environment: Environment<E, A>,
    val agents: List<Agent<A, E>>
)

data class AgentEnvironmentPair<A, E>(
    val agent: Agent<A, E>,
    val environment: Environment<E, A>
)

class MCCFramework<A, E>(
    private val agentQueuePopulation: QueuePopulation<Agent<A, E>>,
    private val environmentQueuePopulation: QueuePopulation<Environment<E, A>>,
    private val dispatcher: CoroutineDispatcher
) {
    var maxAgents = 5.0
    var minAgents = 1.0

    suspend fun iterate(): Pair<List<Agent<A, E>>, List<SolvedEnvironment<E, A>>> = coroutineScope {
        // Generate a new batch of mutated agents and environments
        val agentBatch = agentQueuePopulation.selectBatchForMutation()
        val newAgents = agentBatch.map { it.mutate(agentBatch) }
        val environmentBatch = environmentQueuePopulation.selectBatchForMutation()
        val newEnvironments = environmentBatch.map { it.mutate(environmentBatch) }

        // Filter for environments with available resources
        val availableEnvironments =
            environmentQueuePopulation.queue.filter { it.resourceUsageLimit > it.resourceUsageCount }


        val successfulAgents = newAgents.map { agent ->
            async(dispatcher) {
                val environment = availableEnvironments.firstOrNull { environment ->
                    agent.satisfiesMinimalCriterion(environment)
                }
                if (environment != null) {
                    AgentEnvironmentPair(agent, environment)
                } else null
            }
        }.awaitAll().filterNotNull()
            .onEach {
                if (it.environment.resourceUsageCount < it.environment.resourceUsageLimit)
                    it.environment.resourceUsageCount++
            }
            .map { it.agent }

        // Evaluate all agents (including new successful ones) against new environments
        val allAgents = agentQueuePopulation.queue + successfulAgents
        val successfulEnvironments = newEnvironments.map { environment ->
            async(dispatcher) {
                SolvedEnvironment(environment, environment.testAgents(allAgents))
            }
        }.awaitAll().filter { it.agents.size in (minAgents.toInt()..maxAgents.toInt()) }
        if (successfulEnvironments.size == 0) {
            maxAgents += .1
        } else {
            maxAgents -= .1
            maxAgents = maxAgents.coerceAtLeast(max(minAgents, 1.0))
        }
        if (successfulAgents.size == 0) {
            minAgents -= 1.0
            minAgents = minAgents.coerceAtLeast(1.0)
        } else {
            minAgents += .1
            minAgents = minAgents.coerceIn(1.0, maxAgents)
        }
        // Add successful offspring back to their respective queues

        agentQueuePopulation.addToQueue(successfulAgents)
        environmentQueuePopulation.addToQueue(successfulEnvironments.map { it.environment })

        Pair(successfulAgents, successfulEnvironments)
    }
}
