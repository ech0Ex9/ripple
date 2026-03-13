plugins {
    id("org.jetbrains.intellij") version "1.17.0"
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation(project(":detector-api"))
    implementation(project(":detectors:spring-mvc"))
    implementation(project(":detectors:grpc"))
    implementation(project(":detectors:mq"))
    implementation(project(":detectors:scheduled"))
    implementation(project(":detectors:custom"))
    implementation(Libs.Kotlin.STDLIB)
    implementation(Libs.Kotlin.COROUTINES)
}

intellij {
    version.set("2025.3")
    type.set("IC")
}

tasks.named("buildSearchableOptions") {
    enabled = false
}
