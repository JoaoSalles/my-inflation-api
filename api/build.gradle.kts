
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.salles"
version = "1.0.0"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.insert.koin.koinKtor)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.slf4j.api)
    implementation(ktorLibs.http)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.utils)

    runtimeOnly(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.postgresql)

    testImplementation(kotlin("test"))
    testImplementation(libs.flyway.core)
    testImplementation(libs.hikaricp)
    testImplementation(libs.koin.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(ktorLibs.http)
    testImplementation(ktorLibs.serialization)
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.utils)
}
