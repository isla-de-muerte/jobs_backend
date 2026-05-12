plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.4.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.4.3")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.4.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.4.3")
    implementation("io.ktor:ktor-server-auth-jvm:3.4.3")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:3.4.3")
    implementation("io.ktor:ktor-server-cors-jvm:3.4.3")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.4.3")

    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")

    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("com.auth0:java-jwt:4.4.0")
    implementation("de.mkammerer:argon2-jvm:2.11")

    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("io.ktor:ktor-server-cors:3.4.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.4.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.3")
    implementation("io.ktor:ktor-server-status-pages:3.4.3")
    implementation("io.ktor:ktor-server-auth:3.4.3")
    implementation("io.ktor:ktor-server-auto-head-response:3.4.3")
    implementation("io.ktor:ktor-server-request-validation:3.4.3")
    implementation("io.ktor:ktor-server-resources:3.4.3")
    implementation("io.ktor:ktor-server-sse:3.4.3")
    implementation("io.ktor:ktor-server-webjars:3.4.3")
    implementation("org.webjars:jquery:3.2.1")

    testImplementation(kotlin("test"))
}
