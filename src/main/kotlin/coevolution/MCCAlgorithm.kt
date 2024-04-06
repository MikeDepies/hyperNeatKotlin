package coevolution

import kotlinx.coroutines.*
import kotlin.random.Random

data class SolvedEnvironment<E, A>(
    val environment: Environment<E, A>,
    val agents: List<Agent<A, E>>
)

data class AgentEnvironmentPair<A, E>(
    val agent: Agent<A, E>,
    val environment: Environment<E, A>
)
suspend fun <T, R> List<T>.asyncMapAndJoin(
    dispatcher: CoroutineDispatcher,
    transform: suspend (T) -> R
): List<R> = coroutineScope {
    map { item ->
        async(dispatcher) { transform(item) }
    }.awaitAll()
}

suspend fun <T> List<T>.asyncFilterAndJoin(
    dispatcher: CoroutineDispatcher,
    predicate: suspend (T) -> Boolean
): List<T> = coroutineScope {
    map { item ->
        async(dispatcher) { item to predicate(item) }
    }.awaitAll().filter { it.second }.map { it.first }
}


class MCCFramework<A, E>(
    private val random: Random,
    private val agentQueuePopulation: QueuePopulation<Agent<A, E>>,
    private val environmentQueuePopulation: QueuePopulation<Environment<E, A>>,
    private val dispatcher: CoroutineDispatcher,
    private val environmentMCTest: (MCCFramework<A, E>, SolvedEnvironment<E, A>) -> Boolean,
    var maxAgents: Double = 5.0,
    var minAgents: Double = 1.0
) {
    
    suspend fun iterate(): Pair<List<Agent<A, E>>, List<SolvedEnvironment<E, A>>> = coroutineScope {
        // Generate a new batch of mutated agents and environments
        val agentBatch = agentQueuePopulation.selectBatchForMutation()
        val newAgents = agentBatch.asyncMapAndJoin(dispatcher) { it.mutate(agentBatch) }
        val environmentBatch = environmentQueuePopulation.selectBatchForMutation()
        val newEnvironments = environmentBatch.asyncMapAndJoin(dispatcher) { it.mutate(environmentBatch) }

        // Filter for environments with available resources
        val availableEnvironments =
            environmentQueuePopulation.queue.filter { it.resourceUsageLimit > it.resourceUsageCount }


        val successfulAgents = newAgents.asyncMapAndJoin(dispatcher) { agent ->
            
                val environment = availableEnvironments.shuffled(random).firstOrNull { environment ->
                    agent.satisfiesMinimalCriterion(environment)
                }
                if (environment != null) {
                    AgentEnvironmentPair(agent, environment)
                } else null
            
        }.filterNotNull()
            .onEach {
                if (it.environment.resourceUsageCount < it.environment.resourceUsageLimit)
                    it.environment.resourceUsageCount++
            }
            .map { it.agent }

        // Evaluate all agents (including new successful ones) against new environments
        val allAgents = agentQueuePopulation.queue// + successfulAgents
        val successfulEnvironments = newEnvironments.asyncMapAndJoin(dispatcher) { environment ->
            SolvedEnvironment(environment, environment.testAgents(allAgents))
        }.asyncFilterAndJoin(dispatcher) { 
            environmentMCTest(this@MCCFramework, it) 
        }
       
        // Add successful offspring back to their respective queues

        agentQueuePopulation.addToQueue(successfulAgents)
        environmentQueuePopulation.addToQueue(successfulEnvironments.map { it.environment })

        Pair(successfulAgents, successfulEnvironments)
    }
}
