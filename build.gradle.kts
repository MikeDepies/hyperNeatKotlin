plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
}

group = "com.github.mikedepies"
version = "0.1.0"
val kotest = "5.8.0"

repositories {
    mavenLocal()
    mavenCentral()
}


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1-Beta")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.kotest:kotest-runner-junit5:$kotest")
    testImplementation ("io.kotest:kotest-assertions-core:$kotest")
    testImplementation ("io.kotest:kotest-property:$kotest")
}

tasks.test {
    useJUnitPlatform()
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.github.MikeDepies"
            artifactId = "hyperNeatAi"
            version = "0.1.0"
        }
    }
}
