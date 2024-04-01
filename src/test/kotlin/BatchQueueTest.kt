import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import coevolution.BatchQueuePopulation
import java.lang.Integer.min

class BatchQueuePopulationTest : BehaviorSpec({
    given("a BatchQueuePopulation") {
        val populationSize = 100


        `when`("initialized") {
            val batchQueuePopulation = BatchQueuePopulation<Int>(populationSize, 10)
            then("it should have an empty queue") {
                batchQueuePopulation.queue.shouldBeEmpty()
            }
            then("batchNumbers should be empty") {
                batchQueuePopulation.batchNumbers.shouldBeEmpty()
            }
            and("currentBatchNumber should be 0") {
                batchQueuePopulation.currentBatchNumber shouldBe 0
            }
        }

        `when`("individuals are added to the queue") {
            val batchQueuePopulation = BatchQueuePopulation<Int>(populationSize, 10)
            batchQueuePopulation.addToQueue(listOf(1, 2, 3))
            then("the queue should contain the added individuals") {
                batchQueuePopulation.queue shouldHaveSize 3
            }
            and("batchNumbers should be updated accordingly") {
                batchQueuePopulation.batchNumbers shouldHaveSize 3
                batchQueuePopulation.batchNumbers.all { it == batchQueuePopulation.currentBatchNumber - 1 } shouldBe true
            }
        }

        `when`("individuals are added to the queue multiple times") {
            val batchQueuePopulation = BatchQueuePopulation<Int>(populationSize, 10)
            batchQueuePopulation.addToQueue(listOf(1, 2, 3))
            batchQueuePopulation.addToQueue(listOf(4, 5, 6))
            then("currentBatchNumber should be incremented correctly") {
                batchQueuePopulation.currentBatchNumber shouldBe 2
            }
        }

        `when`("more individuals are added than the populationSize allows") {
            val batchQueuePopulation = BatchQueuePopulation<Int>(populationSize, 10)
            repeat(12) { batchQueuePopulation.addToQueue(List(10) { it }) } // Adding more than populationSize
            then("it should remove the oldest individuals to maintain the size constraint") {
                batchQueuePopulation.queue shouldHaveSize populationSize
                // Further assertions can be made about the content of the queue and batchNumbers
            }
        }

        `when`("the queue exceeds the population size") {
            val batchQueuePopulation = BatchQueuePopulation<Int>(populationSize, 10)
            repeat(11) { batchQueuePopulation.addToQueue(List(10) { it }) } // This should trigger the removal of the oldest batch
            then("the oldest batch should be correctly removed") {
                batchQueuePopulation.queue shouldHaveSize populationSize
                batchQueuePopulation.batchNumbers.first shouldBe 1 // Assuming batch number starts at 0
            }
        }

        `when`("operations are performed on the queue") {
            val batchQueuePopulation = BatchQueuePopulation<Int>(populationSize, 10)
            batchQueuePopulation.addToQueue(listOf(1, 2, 3))
            batchQueuePopulation.selectBatchForMutation()
            then("the size of the queue and batchNumbers should remain consistent") {
                batchQueuePopulation.queue.size shouldBe batchQueuePopulation.batchNumbers.size
            }
        }

        `when`("selecting a batch for mutation") {
            val batchQueuePopulation = BatchQueuePopulation<Int>(populationSize, 10)
            val initialQueue = batchQueuePopulation.queue.toList() // Capture the initial state of the queue
            val batchForMutation = batchQueuePopulation.selectBatchForMutation()
            then("it should select the correct number of individuals for mutation") {
                val expectedBatchSize = min(populationSize / 10, batchQueuePopulation.queue.size + batchForMutation.size) // Adjusted to account for the removal during selection
                batchForMutation shouldHaveSize expectedBatchSize
            }
            and("it should move the selected individuals to the back of the queue") {
                val expectedQueue = initialQueue.drop(batchForMutation.size) + batchForMutation
                batchQueuePopulation.queue.toList() shouldBe expectedQueue
            }
            
        }
        

        `when`("the queue is empty") {
            val emptyQueuePopulation = BatchQueuePopulation<Int>(populationSize, 10)
            val batchForMutation = emptyQueuePopulation.selectBatchForMutation()
            then("selectBatchForMutation should return an empty list") {
                batchForMutation.shouldBeEmpty()
            }
        }

        `when`("an empty list is added to the queue") {
            val batchQueuePopulation = BatchQueuePopulation<Int>(populationSize, 10)
            val initialBatchNumber = batchQueuePopulation.currentBatchNumber
            batchQueuePopulation.addToQueue(emptyList())
            then("currentBatchNumber should not change") {
                batchQueuePopulation.currentBatchNumber shouldBe initialBatchNumber
            }
            and("the queue and batchNumbers should remain unchanged") {
                batchQueuePopulation.queue.shouldBeEmpty()
                batchQueuePopulation.batchNumbers.shouldBeEmpty()
            }
        }

        // Additional tests for batch number incrementation and correct batch removal can be added here
    }
})