package coevolution

import environment.Position
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class AgentSolution<A, E>(val agent: Agent<A, E>, val solution: List<Position>)
interface SolutionMap<A, E> {
    fun addSolution(agent: AgentEnvironmentPair<A, E>, solution: List<Position>)
    fun getSolution(environment: Environment<E, A>): List<AgentSolution<A, E>>?
    fun clearSolutions()
}

sealed class SolutionMapCommand<A, E>
data class AddSolutionCommand<A, E>(val agent: AgentEnvironmentPair<A, E>, val solution: List<Position>) :
    SolutionMapCommand<A, E>()

// data class GetSolutionCommand<A, E>(val environment: Environment<E, A>) : SolutionMapCommand<A, E>()
object ClearSolutionsCommand : SolutionMapCommand<Nothing, Nothing>()
class SolutionMapCommandSender<A, E>(private val solutionChannel: Channel<SolutionMapCommand<A, E>>) {
    fun addSolution(agent: AgentEnvironmentPair<A, E>, solution: List<Position>) {
        solutionChannel.trySend(AddSolutionCommand(agent, solution))
    }

    // fun getSolution(environment: Environment<E, A>) {
    //     solutionChannel.trySend(GetSolutionCommand(environment))
    // }

    fun clearSolutions() {
        solutionChannel.trySend(ClearSolutionsCommand as SolutionMapCommand<A, E>)
    }
}

class SolutionMapSimple<A, E> : SolutionMap<A, E> {
    private val map: MutableMap<Environment<E, A>, MutableList<AgentSolution<A, E>>> = mutableMapOf()

    override fun addSolution(agent: AgentEnvironmentPair<A, E>, solution: List<Position>) {
        val currentSolutions = map.getOrPut(agent.environment) { mutableListOf() }
        currentSolutions += AgentSolution(agent.agent, solution)
    }

    override fun getSolution(environment: Environment<E, A>): List<AgentSolution<A, E>>? {
        return map[environment]
    }

    override fun clearSolutions() {
        map.clear()
    }
}

class DelegatedSolutionMap<A, E>(
    private val solutionMapUpdater: SolutionMapUpdater<A, E>,
    private val solutionMapCommandSender: SolutionMapCommandSender<A, E>,

    ) : SolutionMap<A, E> {
    override fun addSolution(agent: AgentEnvironmentPair<A, E>, solution: List<Position>) {
        solutionMapCommandSender.addSolution(agent, solution)
    }

    override fun getSolution(environment: Environment<E, A>): List<AgentSolution<A, E>>? {
        return solutionMapUpdater.map[environment]
    }

    override fun clearSolutions() {
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
                // is GetSolutionCommand -> {
                //     // Implement get solution logic
                // }
                is ClearSolutionsCommand -> {
                    map.clear()
                }
            }
        }
    }

}