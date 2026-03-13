plugins {
    kotlin("jvm")
}

dependencies {
    implementation(Libs.Kotlin.STDLIB)
    implementation(Libs.Kotlin.COROUTINES)
    
    testImplementation(Libs.Test.JUNIT_API)
    testImplementation(Libs.Test.JUNIT_ENGINE)
    testImplementation(Libs.Test.MOCKK)
}