plugins {
    kotlin("jvm") version "1.9.22"
}

group = "com.github.mikedepies"
version = "0.1.0"
val kotest = "5.8.0"

repositories {
    mavenCentral()
}


dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.kotest:kotest-runner-junit5:$kotest")
    testImplementation ("io.kotest:kotest-assertions-core:$kotest")
    testImplementation ("io.kotest:kotest-property:$kotest")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(18)
}
kotlin {
    jvmToolchain(18)
}