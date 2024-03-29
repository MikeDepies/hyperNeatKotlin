package genome

enum class ActivationFunction {
    IDENTITY,
    SIGMOID,
    TANH,
    RELU,
    GAUSSIAN,
    SINE,
    COS,
    ABS,
    STEP;
    companion object {
        val cppn = listOf(IDENTITY, SIGMOID, TANH, RELU, GAUSSIAN, SINE, COS, ABS, STEP)
    } 
}




