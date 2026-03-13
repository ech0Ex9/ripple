plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":detector-api"))
    
    testImplementation(Libs.Test.JUNIT_API)
    testImplementation(Libs.Test.JUNIT_ENGINE)
    testImplementation(Libs.Test.MOCKK)
}