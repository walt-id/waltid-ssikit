import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("org.owasp.dependencycheck") version "6.5.3"
    id("com.github.jk1.dependency-license-report") version "2.0"
    application
    `maven-publish`
}

group = "id.walt"
version = "1.SNAPSHOT"

repositories {
    mavenCentral()
    //jcenter()
    maven("https://jitpack.io")
    maven("https://maven.walt.id/repository/waltid/")
    maven("https://maven.walt.id/repository/waltid-ssi-kit/")
    mavenLocal()
}

dependencies {
    // Crypto
    api("com.google.crypto.tink:tink:1.7.0")
    api("info.weboftrust:ld-signatures-java:1.2-SNAPSHOT")
    api("decentralized-identity:jsonld-common-java:1.1.0")
    implementation("com.goterl:lazysodium-java:5.1.4")
    implementation("net.java.dev.jna:jna:5.12.1")
    implementation("com.github.multiformats:java-multibase:v1.1.0")
    implementation("com.microsoft.azure:azure-keyvault:1.2.6")
    implementation("com.microsoft.azure:azure-client-authentication:1.7.14")
    implementation("com.nimbusds:nimbus-jose-jwt:9.30.1")
    implementation("com.nimbusds:oauth2-oidc-sdk:10.5.1")

    implementation("org.bouncycastle:bcprov-jdk15to18:1.72")
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.72")

    // Ethereum
    implementation("org.web3j:core:5.0.0")
    implementation("org.web3j:crypto:5.0.0")

    implementation("com.google.guava:guava:31.1-jre")

    // JSON
    implementation("org.json:json:20220924")
    implementation("com.beust:klaxon:5.6")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
    implementation("com.jayway.jsonpath:json-path:2.7.0")
    implementation("com.networknt:json-schema-validator:1.0.77")
    implementation("net.pwall.json:json-kotlin-schema:0.39")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    // DB
    implementation("org.xerial:sqlite-jdbc:3.39.4.1")
    implementation("com.zaxxer:HikariCP:5.0.1")

    // CLI
    implementation("com.github.ajalt.clikt:clikt-jvm:3.5.0")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")

    // Misc
    implementation("commons-io:commons-io:2.11.0")
    implementation("io.minio:minio:8.4.6")

    // HTTP
    implementation("io.ktor:ktor-client-jackson-jvm:2.2.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.4")
    implementation("io.ktor:ktor-client-core-jvm:2.2.4")
    //implementation("io.ktor:ktor-client-cio-jvm:2.2.4")
    implementation("io.ktor:ktor-client-okhttp:2.2.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")
    implementation("io.ktor:ktor-client-logging-jvm:2.2.4")

    // REST
    implementation("io.javalin:javalin:4.6.7")
    implementation("io.javalin:javalin-openapi:4.6.7")
    // implementation("io.javalin:javalin-test-tools:4.5.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.4")
    implementation("org.slf4j:slf4j-simple:2.0.4")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")

    // Config
    implementation("com.sksamuel.hoplite:hoplite-core:2.6.5")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.6.5")
    implementation("com.sksamuel.hoplite:hoplite-hikaricp:2.6.5")

    // Service-Matrix
    implementation("id.walt.servicematrix:WaltID-ServiceMatrix:1.1.3")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")

    // JNR-FFI
    implementation("com.github.jnr:jnr-ffi:2.2.13")

    // Testing
    //testImplementation(kotlin("test-junit"))
    testImplementation("io.mockk:mockk:1.13.2")

    testImplementation("io.kotest:kotest-runner-junit5:5.5.4")
    testImplementation("io.kotest:kotest-assertions-core:5.5.4")
    testImplementation("io.kotest:kotest-assertions-json:5.5.4")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

/*
tasks.withType<Test> {
    useJUnitPlatform()
}
 */

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

tasks.named<CreateStartScripts>("startScripts") {
    doLast {
        windowsScript.writeText(
            windowsScript.readText().replace(Regex("set CLASSPATH=.*"), "set CLASSPATH=%APP_HOME%\\\\lib\\\\*")
        )
    }
}

val fatJar = task("fatJar", type = Jar::class) {
    group = "build"

    archiveBaseName.set("${project.name}-with-dependencies")

    manifest {
        attributes["Implementation-Title"] = "Gradle Jar Bundling"
        attributes["Implementation-Version"] = archiveVersion.get()
        attributes["Main-Class"] = "id.walt.MainCliKt"
    }

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

application {
    mainClass.set("id.walt.MainCliKt")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("walt.id SSI Kit")
                description.set(
                    """
                    Kotlin/Java library for SSI core services, with primary focus on European EBSI/ESSIF ecosystem.
                    """.trimIndent()
                )
                url.set("https://walt.id")
                licenses {
                    license {
                        name.set("Apache 2")
                        url.set("https://raw.githubusercontent.com/walt-id/waltid-ssikit/master/LICENSE")
                    }
                }
            }
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://maven.walt.id/repository/waltid-ssi-kit/")
            val envUsername = System.getenv("MAVEN_USERNAME")
            val envPassword = System.getenv("MAVEN_PASSWORD")

            val usernameFile = File("secret_maven_username.txt")
            val passwordFile = File("secret_maven_password.txt")

            val secretMavenUsername = envUsername ?: usernameFile.let { if (it.isFile) it.readLines().first() else "" }
            //println("Deploy username length: ${secretMavenUsername.length}")
            val secretMavenPassword = envPassword ?: passwordFile.let { if (it.isFile) it.readLines().first() else "" }

            //if (secretMavenPassword.isBlank()) {
            //   println("WARNING: Password is blank!")
            //}

            credentials {
                username = secretMavenUsername
                password = secretMavenPassword
            }
        }
    }
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(InventoryHtmlReportRenderer("ssikit-licenses-report.html","SSI Kit"))
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
}
