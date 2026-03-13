plugins {
    id("org.jetbrains.intellij") version "1.17.0"
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "org.ripple"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":detector-api"))
    implementation(project(":core"))
    implementation(project(":ui"))
    implementation(project(":report"))
    implementation(project(":detectors:spring-mvc"))
    implementation(project(":detectors:grpc"))
    implementation(project(":detectors:mq"))
    implementation(project(":detectors:scheduled"))
    implementation(project(":detectors:custom"))
    
    implementation(Libs.Kotlin.STDLIB)
    implementation(Libs.Kotlin.COROUTINES)
    implementation(Libs.Kotlinx.SERIALIZATION_JSON)
}

intellij {
    version.set("2025.3")
    type.set("IC")
    
    plugins.set(listOf(
        "java",
        "Git4Idea"
    ))
}

tasks {
    patchPluginXml {
        sinceBuild.set("253")
        untilBuild.set("253.*")
    }
    
    named("buildSearchableOptions") {
        enabled = false
    }
}
