plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":detector-api"))
    implementation(Libs.Kotlin.STDLIB)
    implementation(Libs.Kotlinx.SERIALIZATION_JSON)
    
    testImplementation(Libs.Test.JUNIT_API)
    testImplementation(Libs.Test.JUNIT_ENGINE)
}