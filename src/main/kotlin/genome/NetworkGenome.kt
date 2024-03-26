package genome

data class NetworkGenome(val nodeGenomes : List<NodeGenome>, val connectionGenes : List<ConnectionGenome>, var fitness : Double? = null, var sharedFitness: Double? = null, var speciesId: Int? = null)

