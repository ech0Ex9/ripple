plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":detector-api"))
    implementation(Libs.Kotlin.STDLIB)
}