- [x] fix issues found in BiasedCrossMutation which causes exceptions
  - [ ] Double check for issues
- Investigate why parent selection was tripping on same parent being selected despite having a population size of 3
  - [ ] Some changes were made in the crossmutation implemenations. Recheck if resolved.
- [x] Ensure that mutations to weights are done to new instances instead of mutating references
- [x] create a couple of other simple test problems
  - [x] Non-xor problems such as parity
  - [x] Simple classification problems such as iris
- [x] expand on maze problem to include more complex mazes
  - [x] Added randomly generated mazes with small variations
- [ ] implement a maze problem with a continuous action space
- [ ] implement maze problem with a continuous state space
- [x] implement maze problem with sensors
- [ ] port neat network building and execution to python
  - [ ] Test in parallel environments and verify parity
- [ ] Implement Maze generation with CPPN
- [ ] Implement MCC for maze problem
  - MCC (Minimal Criterion coevolution) is a method to evolve a population of agents to solve a problem by evolving a population of problem instances.
  - The problem instances are evolved to be as difficult as possible for the agents to solve.
  - The agents are evolved to solve the problem instances.
  - The variation we'll implement is resource limitation.
  - MCC resources places a limit on the number of times a problem instance can be solved by an agent.
  - This is done to prevent the agents from overfitting to the problem instances.


## Novelty Search
- Novelty search is a method to evolve a population of agents to solve a problem by evolving a population of problem instances.
- The problem instances are evolved to be as different as possible from each other.
- The agents are evolved to solve the problem instances.

## Decouple Evolution from NEAT model 
- Currently the evolution is tightly coupled with the NEAT model.
- This makes it difficult to use the evolution code with other models.
- The goal is to decouple the evolution code from the NEAT model so that it can be used with other models.
- We should be able to use any blackbox function in place of the network  
- This will allow us to use the evolution code with other models such as the CPPN, or entirely non-traditional models


## Other
- fix so no fitness values leak between generations
  - only cross mutations are causing the fitness values and species to get reset
  - stil need to make sure children still have their species at the end of the process