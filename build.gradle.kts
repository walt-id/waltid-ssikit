import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.20"
    kotlin("plugin.serialization") version "1.5.20"
    id("com.github.kkdad.dependency-license-report") version "1.16.6"
    id("org.owasp.dependencycheck") version "6.1.6"
    //id("org.sonatype.gradle.plugins.scan") version "2.0.9"
    //id("org.sonarqube") version "3.2.0"
    application
    `maven-publish`
}

group = "id.walt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    //jcenter()
    maven("https://jitpack.io")
    maven("https://repo.danubetech.com/repository/maven-public/")
    maven("https://maven.walt.id/repository/waltid/")
    maven("https://maven.walt.id/repository/waltid-ssi-kit/")
    maven("https://maven.walt.id/repository/danubetech")
    //mavenLocal()
}

dependencies {
    // Crypto
    api("com.google.crypto.tink:tink:1.6.1")
    api("info.weboftrust:ld-signatures-java:0.5-SNAPSHOT")
    api("decentralized-identity:jsonld-common-java:0.2.0")
    implementation("com.goterl:lazysodium-java:5.0.1")
    implementation("com.github.multiformats:java-multibase:v1.1.0")
    implementation("com.microsoft.azure:azure-keyvault:1.2.4")
    implementation("com.microsoft.azure:azure-client-authentication:1.7.13")

    // Ethereum
    implementation("org.web3j:core:5.0.0")
    implementation("org.web3j:crypto:5.0.0")

    implementation("com.google.guava:guava:30.1.1-jre")

    // VC
    implementation("id.walt:waltid-ssikit-vclib:1.4.3")

    // JSON
    implementation("org.json:json:20210307")
    implementation("com.beust:klaxon:5.5")
    //implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")

    // DB
    implementation("org.xerial:sqlite-jdbc:3.36.0.1")
    implementation("com.zaxxer:HikariCP:5.0.0")

    // CLI
    implementation("com.github.ajalt.clikt:clikt-jvm:3.2.0")
    implementation("com.github.ajalt.clikt:clikt:3.2.0")

    // Misc
    implementation("commons-io:commons-io:2.11.0")

    // HTTP
    implementation("io.ktor:ktor-client-core:1.6.1")
    implementation("io.ktor:ktor-client-cio:1.6.1")
    implementation("io.ktor:ktor-client-serialization:1.6.1")
    implementation("io.ktor:ktor-client-logging:1.6.1")

    // REST
    implementation("io.javalin:javalin-bundle:3.13.9")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.0-alpha2")
    implementation("org.apache.logging.log4j:log4j-core:2.14.1")
    implementation("org.apache.logging.log4j:log4j-api:2.14.1")
    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.14.1")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.10")

    // Config
    implementation("com.sksamuel.hoplite:hoplite-core:1.4.4")
    implementation("com.sksamuel.hoplite:hoplite-yaml:1.4.4")
    implementation("com.sksamuel.hoplite:hoplite-hikaricp:1.4.4")

    // Service-Matrix
    implementation("id.walt.servicematrix:WaltID-ServiceMatrix:1.0.1")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.21")

    // Testing
    //testImplementation(kotlin("test-junit"))
    testImplementation("io.mockk:mockk:1.12.0")

    testImplementation("io.kotest:kotest-runner-junit5:4.6.1")
    testImplementation("io.kotest:kotest-assertions-core:4.6.1")
    testImplementation("io.kotest:kotest-assertions-json:4.6.1")
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
        languageVersion.set(JavaLanguageVersion.of(16))
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
                name.set("Walt.ID SSI-Kit")
                description.set("Kotlin/Java library for SSI core services, with primary focus on European EBSI/ESSIF ecosystem.")
                url.set("https://walt.id")
            }
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://maven.walt.id/repository/waltid-ssi-kit/")
            val usernameFile = File("secret_maven_username.txt")
            val passwordFile = File("secret_maven_password.txt")
            val secretMavenUsername = System.getenv()["SECRET_MAVEN_USERNAME"] ?: if (usernameFile.isFile) { usernameFile.readLines()[0] } else { "" }
            val secretMavenPassword = System.getenv()["SECRET_MAVEN_PASSWORD"] ?: if (passwordFile.isFile) { passwordFile.readLines()[0] } else { "" }

            credentials {
                username = secretMavenUsername
                password = secretMavenPassword
            }
        }
    }
}

licenseReport {
    renderers = arrayOf<com.github.jk1.license.render.ReportRenderer>(
        com.github.jk1.license.render.InventoryHtmlReportRenderer(
            "report.html",
            "Backend"
        )
    )
    filters =
        arrayOf<com.github.jk1.license.filter.DependencyFilter>(com.github.jk1.license.filter.LicenseBundleNormalizer())
}

