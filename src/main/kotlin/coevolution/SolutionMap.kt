package coevolution

import environment.Position
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel


import java.util.concurrent.CompletableFuture

data class AgentSolution<A, E>(val agent: Agent<A, E>, val solution: List<Position>)
interface SolutionMap<A, E> {
    suspend fun addSolution(agent: AgentEnvironmentPair<A, E>, solution: List<Position>)
    suspend fun getSolution(environment: Environment<E, A>): List<AgentSolution<A, E>>?
    suspend fun clearSolutions()
}

sealed class SolutionMapCommand<A, E>
data class AddSolutionCommand<A, E>(val agent: AgentEnvironmentPair<A, E>, val solution: List<Position>) :
    SolutionMapCommand<A, E>()
    data class GetSolutionCommand<A, E>(val environment: Environment<E, A>,val deferred: CompletableDeferred<List<AgentSolution<A, E>>?>) : SolutionMapCommand<A, E>()
object ClearSolutionsCommand : SolutionMapCommand<Nothing, Nothing>()
class SolutionMapCommandSender<A, E>(private val solutionChannel: Channel<SolutionMapCommand<A, E>>) {
    suspend fun addSolution(agent: AgentEnvironmentPair<A, E>, solution: List<Position>) {
        solutionChannel.send(AddSolutionCommand(agent, solution))
    }

    suspend fun getSolution(environment: Environment<E, A>, promise: CompletableDeferred<List<AgentSolution<A, E>>?>) {
        solutionChannel.send(GetSolutionCommand(environment, promise))
    }

    suspend fun clearSolutions() {
        solutionChannel.send(ClearSolutionsCommand as SolutionMapCommand<A, E>)
    }
}

class SolutionMapSimple<A, E> : SolutionMap<A, E> {
    private val map: MutableMap<Environment<E, A>, MutableList<AgentSolution<A, E>>> = mutableMapOf()

    override suspend fun addSolution(agent: AgentEnvironmentPair<A, E>, solution: List<Position>) {
        val currentSolutions = map.getOrPut(agent.environment) { mutableListOf() }
        currentSolutions += AgentSolution(agent.agent, solution)
    }

    override suspend fun getSolution(environment: Environment<E, A>): List<AgentSolution<A, E>>? {
        return map[environment]
    }

    override suspend fun clearSolutions() {
        map.clear()
    }
}

class DelegatedSolutionMap<A, E>(
    private val solutionMapUpdater: SolutionMapUpdater<A, E>,
    private val solutionMapCommandSender: SolutionMapCommandSender<A, E>,

    ) : SolutionMap<A, E> {
    override suspend fun addSolution(agent: AgentEnvironmentPair<A, E>, solution: List<Position>) {
        solutionMapCommandSender.addSolution(agent, solution)
    }

    override suspend fun getSolution(environment: Environment<E, A>): List<AgentSolution<A, E>>? {
        val deferred = CompletableDeferred<List<AgentSolution<A, E>>?>()
        solutionMapCommandSender.getSolution(environment, deferred)
        return deferred.await()
    }

    override suspend fun clearSolutions() {
        solutionMapCommandSender.clearSolutions()
    }
}

class SolutionMapUpdater<A, E>(
    private val solutionChannel: Channel<SolutionMapCommand<A, E>>,
    val map: MutableMap<Environment<E, A>, MutableList<AgentSolution<A, E>>>,
) {
    suspend fun init() {
        for (command in solutionChannel) {
            when (command) {
                is AddSolutionCommand -> {
                    val currentSolutions = map.getOrPut(command.agent.environment) { mutableListOf() }
                    currentSolutions += AgentSolution(command.agent.agent, command.solution)
                }
                is GetSolutionCommand -> {
                    val solution = map[command.environment]
                    // Fulfill the promise with the solution
                    command.deferred.complete(solution)
                }
                is ClearSolutionsCommand -> {
                    map.clear()
                }
            }
        }
    }

}