object Versions {
    const val KOTLIN = "1.9.22"
    const val INTELLIJ_PLUGIN = "1.17.0"
    const val JUNIT = "5.10.1"
    const val MOCKK = "1.13.8"
    const val KOTLINX_SERIALIZATION = "1.6.2"
    const val APACHE_POI = "5.2.5"
}

object Libs {
    object Kotlin {
        const val STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}"
        const val COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
    }
    
    object Kotlinx {
        const val SERIALIZATION_JSON = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION}"
    }
    
    object Test {
        const val JUNIT_API = "org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}"
        const val JUNIT_ENGINE = "org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}"
        const val MOCKK = "io.mockk:mockk:${Versions.MOCKK}"
    }
    
    object Apache {
        const val POI = "org.apache.poi:poi-ooxml:${Versions.APACHE_POI}"
    }
}