plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.salles"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.kotlinx.serialization.core)
    implementation(ktorLibs.serialization.kotlinx.json)
}