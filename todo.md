- fix issues found in BiasedCrossMutation which causes exceptions
- Investigate why parent selection was tripping on same parent being selected despite having a population size of 3
- Ensure that mutations to weights are done to new instances instead of mutating references
- create a couple of other simple test problems
  - Non-xor problems such as parity
  - Simple classification problems such as iris

