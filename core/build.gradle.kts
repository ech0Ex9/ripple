plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":detector-api"))
    
    implementation(Libs.Kotlin.STDLIB)
    implementation(Libs.Kotlin.COROUTINES)
    implementation(Libs.Kotlinx.SERIALIZATION_JSON)
    
    testImplementation(Libs.Test.JUNIT_API)
    testImplementation(Libs.Test.JUNIT_ENGINE)
    testImplementation(Libs.Test.MOCKK)
}