package coevolution

interface QueuePopulation<T> {
    val queue: MutableList<T>
    fun selectBatchForMutation(): List<T>
    fun addToQueue(individuals: List<T>)
}