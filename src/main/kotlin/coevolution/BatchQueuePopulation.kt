package coevolution

import java.util.*
import kotlin.math.min

class BatchQueuePopulation<T>(private val populationSize: Int, private val batchSize: Int) : QueuePopulation<T> {
    override val queue: LinkedList<T> = LinkedList()
    val batchNumbers: LinkedList<Int> = LinkedList()
    var currentBatchNumber = 0

    override fun selectBatchForMutation(): List<T> {
        val batchSize = min(batchSize, queue.size)
        val batchForMutation = mutableListOf<T>()
        repeat(batchSize) {
            queue.peekFirst()?.let {
                batchForMutation.add(it)
                queue.removeFirst()
                queue.addLast(it) // Move the selected individual to the back of the queue
            }
        }
        return batchForMutation
    }

    override fun addToQueue(individuals: List<T>) {
        if (individuals.isNotEmpty()) {
            queue.addAll(individuals)
            repeat(individuals.size) { batchNumbers.addLast(currentBatchNumber) }
            currentBatchNumber++

            while (queue.size > populationSize) {
                val oldestBatchIndex = batchNumbers.indexOf(batchNumbers.peekFirst())
                queue.removeAt(oldestBatchIndex)
                batchNumbers.removeFirst()
            }
        }
    }
}