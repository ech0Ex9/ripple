rootProject.name = "idea-endpoint-plugin"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(
    "detector-api",
    "core",
    "detectors:spring-mvc",
    "detectors:grpc",
    "detectors:mq",
    "detectors:scheduled",
    "ui",
    "report",
    "plugin",
    "tests"
)