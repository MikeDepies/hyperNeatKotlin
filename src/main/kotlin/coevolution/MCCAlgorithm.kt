package coevolution

import environment.renderEnvironmentAsString

class MCCFramework<A, E>(
    private val agentQueuePopulation: QueuePopulation<Agent<A, E>>,
    private val environmentQueuePopulation: QueuePopulation<Environment<E, A>>
) {
    
    fun iterate(): Pair<List<Agent<A, E>>, List<Environment<E, A>>> {
        // Generate a new batch of mutated agents and environments
        val newAgents = agentQueuePopulation.selectBatchForMutation().map { it.mutate() }
        val newEnvironments = environmentQueuePopulation.selectBatchForMutation().map { it.mutate() }

        // Filter for environments with available resources
        val availableEnvironments = environmentQueuePopulation.queue.filter { it.resourceUsageLimit > it.resourceUsageCount }

        val successfulAgents = newAgents.filter { agent ->
            availableEnvironments.any { environment ->
                // println("Testing environment for agent")
                agent.satisfiesMinimalCriterion(environment).also { satisfies ->
                    if (satisfies) environment.resourceUsageCount++
                }
            }
        }

        // Evaluate all agents (including new successful ones) against new environments
        val allAgents = agentQueuePopulation.queue + successfulAgents
        val successfulEnvironments = newEnvironments.filter { environment ->
            
            allAgents.any { agent -> environment.satisfiesMinimalCriterion(agent) }
        }
        
        // Add successful offspring back to their respective queues
       
        agentQueuePopulation.addToQueue(successfulAgents)
        environmentQueuePopulation.addToQueue(successfulEnvironments)
        
        return Pair(successfulAgents, successfulEnvironments)
    }
}
