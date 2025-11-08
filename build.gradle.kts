plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("io.ktor.plugin") version "2.3.7"
    id("org.flywaydb.flyway") version "9.22.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.codewithfk"
version = "0.0.1"

application {
    mainClass.set("com.codewithfk.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-cors-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm")
    implementation("io.micrometer:micrometer-registry-prometheus:1.11.5")
    
    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.44.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.44.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.44.1")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:5.0.1")
    
    // Security
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("com.auth0:java-jwt:4.4.0")
    
    // Email
    implementation("com.sun.mail:javax.mail:1.6.2")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.21")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("io.mockk:mockk:1.13.8")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}

flyway {
    url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/quickernotes"
    user = System.getenv("DB_USER") ?: "postgres"
    password = System.getenv("DB_PASSWORD") ?: "password"
    locations = arrayOf("filesystem:src/main/resources/db/migration")
}
kotlin {
    jvmToolchain(21)
}

// Configure shadow JAR for Heroku deployment
tasks.shadowJar {
    archiveBaseName.set("quickernotes-server")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes(mapOf("Main-Class" to "com.codewithfk.ApplicationKt"))
    }
}

// Heroku stage task - builds the application
tasks.register("stage") {
    dependsOn("shadowJar")
    description = "Build the application for Heroku deployment"
    doLast {
        println("Application built successfully for Heroku deployment")
        println("JAR location: build/libs/quickernotes-server.jar")
    }
}