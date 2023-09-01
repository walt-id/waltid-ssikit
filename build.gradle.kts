import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    id("org.owasp.dependencycheck") version "8.4.0"
    id("com.github.jk1.dependency-license-report") version "2.5"
    application
    `maven-publish`
}

group = "id.walt"
version = "1.1.1"

repositories {
    mavenCentral()
    //jcenter()
    maven("https://jitpack.io")
    maven("https://maven.walt.id/repository/waltid/")
    maven("https://maven.walt.id/repository/waltid-ssi-kit/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")

    maven("https://repo.danubetech.com/repository/maven-public/")
    mavenLocal()
}

dependencies {
    // Crypto
    api("com.google.crypto.tink:tink:1.10.0")
    api("info.weboftrust:ld-signatures-java:1.5.0")
    api("decentralized-identity:jsonld-common-java:1.5.0")
    implementation("com.goterl:lazysodium-java:5.1.4")
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("com.github.multiformats:java-multibase:v1.1.1")
    implementation("com.microsoft.azure:azure-keyvault:1.2.6")
    implementation("com.microsoft.azure:azure-client-authentication:1.7.14")
    implementation("com.nimbusds:nimbus-jose-jwt:9.31")
    implementation("com.nimbusds:oauth2-oidc-sdk:10.11") // changes to authorization_details in 10.12+ not supported
    implementation("id.walt:waltid-sd-jwt-jvm:1.2306191408.0")

    implementation("org.bouncycastle:bcprov-jdk18on:1.76")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.76")

    // Ethereum
    implementation("org.web3j:core:4.10.2") // 5.0.0 is older than 4.x (wrong release)
    implementation("org.web3j:crypto:4.10.2")

    implementation("com.google.guava:guava:32.1.2-jre")

    // JSON
    implementation("org.json:json:20230618")
    implementation("com.beust:klaxon:5.6")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    implementation("com.jayway.jsonpath:json-path:2.8.0")
    implementation("com.networknt:json-schema-validator:1.0.86")
    implementation("net.pwall.json:json-kotlin-schema:0.40")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // DB
    implementation("org.xerial:sqlite-jdbc:3.43.0.0")
    implementation("com.zaxxer:HikariCP:5.0.1")

    // CLI-SNAPSHOT
    implementation("com.github.ajalt.clikt:clikt-jvm:4.2.0")
    implementation("com.github.ajalt.clikt:clikt:4.2.0")

    // Misc
    implementation("commons-io:commons-io:2.13.0")
    implementation("io.minio:minio:8.5.5")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // HTTP
    implementation("io.ktor:ktor-client-jackson-jvm:2.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.3")
    implementation("io.ktor:ktor-client-core-jvm:2.3.3")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.3")
    implementation("io.ktor:ktor-client-auth-jvm:2.3.3")
    implementation("io.ktor:ktor-client-okhttp-jvm:2.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.3")
    implementation("io.ktor:ktor-client-logging-jvm:2.3.3")

    // REST
    implementation("io.javalin:javalin:4.6.8")
    implementation("io.javalin:javalin-openapi:4.6.8")
    // implementation("io.javalin:javalin-test-tools:4.5.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.slf4j:slf4j-simple:2.0.7")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Config
    implementation("com.sksamuel.hoplite:hoplite-core:2.7.5")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.7.5")
    implementation("com.sksamuel.hoplite:hoplite-hikaricp:2.7.5")

    // Service-Matrix
    implementation("id.walt.servicematrix:WaltID-ServiceMatrix:1.1.3")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")

    // JNR-FFI
    implementation("com.github.jnr:jnr-ffi:2.2.14")

    // Testing
    //testImplementation(kotlin("test-junit"))
    testImplementation("io.mockk:mockk:1.13.7")

    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
    testImplementation("io.kotest:kotest-assertions-core:5.6.2")
    testImplementation("io.kotest:kotest-assertions-json:5.6.2")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")

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
    kotlinOptions.jvmTarget = "17"
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
    renderers = arrayOf<ReportRenderer>(InventoryHtmlReportRenderer("ssikit-licenses-report.html", "SSI Kit"))
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
}
