
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
    implementation(project(":domain"))
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.flyway.core)
    implementation(libs.hikaricp)
    implementation(libs.ktor.client.okhttp)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)

    runtimeOnly("io.netty:netty-common:4.2.9.Final")
    runtimeOnly("io.netty:netty-handler:4.2.9.Final")
    runtimeOnly("org.eclipse.jetty.alpn:alpn-api:1.1.3.v20160715")
    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.kotlinx.coroutines.test)

    testRuntimeOnly("org.jetbrains.kotlin:kotlin-test-junit:2.3.20")
    testRuntimeOnly(ktorLibs.client.apache5)
    testRuntimeOnly(ktorLibs.client.cio)
    testRuntimeOnly(libs.testcontainers.core)
    testRuntimeOnly(libs.testcontainers.postgresql)
}