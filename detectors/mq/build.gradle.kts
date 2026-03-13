plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":detector-api"))
    implementation(Libs.Kotlin.STDLIB)
    
    testImplementation(Libs.Test.JUNIT_API)
    testImplementation(Libs.Test.JUNIT_ENGINE)
    testImplementation(Libs.Test.MOCKK)
}