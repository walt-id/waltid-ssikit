import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    kotlin("plugin.serialization") version "1.4.30"
    application
    `maven-publish`
}

group = "org.letstrust"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    maven("https://repo.danubetech.com/repository/maven-snapshots/")
}

dependencies {
    // Crypto
    implementation("com.google.crypto.tink:tink:1.5.0")
    implementation("info.weboftrust:ld-signatures-java:0.4-SNAPSHOT")
    implementation("com.goterl.lazycode:lazysodium-java:4.3.2")
    implementation("com.github.multiformats:java-multibase:v1.1.0")

    implementation("com.google.guava:guava:30.1.1-jre")

    // JSON
    implementation("org.json:json:20210307")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")

    // DB
    implementation("org.xerial:sqlite-jdbc:3.34.0")
    implementation("com.zaxxer:HikariCP:4.0.3")

    // CLI
    implementation("com.github.ajalt.clikt:clikt-jvm:3.1.0")
    implementation("com.github.ajalt.clikt:clikt:3.1.0")

    // Misc
    implementation("commons-io:commons-io:2.6")

    // HTTP
    implementation("khttp:khttp:1.0.0")
    implementation("io.ktor:ktor-client-core:1.5.2")
    implementation("io.ktor:ktor-client-cio:1.5.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.0-alpha1")

    implementation("org.apache.logging.log4j:log4j-core:2.14.1")
    implementation("org.apache.logging.log4j:log4j-api:2.14.1")
    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.14.1")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")

    // Config
    implementation("com.sksamuel.hoplite:hoplite-core:1.4.0")
    implementation("com.sksamuel.hoplite:hoplite-yaml:1.4.0")
    implementation("com.sksamuel.hoplite:hoplite-hikaricp:1.4.0")

    // Testing
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "13"
}

val fatJar = task("fatJar", type = Jar::class) {
    group = "build"

    archiveBaseName.set("${project.name}-with-dependencies")

    manifest {
        attributes["Implementation-Title"] = "Gradle Jar Bundling"
        attributes["Implementation-Version"] = archiveVersion.get()
        attributes["Main-Class"] = "org.letstrust.MainKt"
    }

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

application {
    mainClass.set("org.letstrust.MainKt")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("Lets Trust SSI Core")
                description.set("Kotlin/Java library for SSI core services, with primary focus on European EBSI/ESSIF ecosystem.")
                url.set("https://letstrust.io")
            }
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://maven.letstrust.io/repository/letstrust-ssi-core/")

            credentials {
                username = "letstrust-build"
                password = "naidohTeiraG9ouzoo0"
            }
        }
    }
}
