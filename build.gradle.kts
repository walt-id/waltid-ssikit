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

group = "org.letstrust"
version = "0.6"

repositories {
    mavenCentral()
    //jcenter()
    maven("https://jitpack.io")
    maven("https://repo.danubetech.com/repository/maven-releases/")
    maven {
        url = uri("https://maven.letstrust.io/repository/waltid/")

        credentials {
            username = "letstrust-build"
            password = "naidohTeiraG9ouzoo0"
        }
    }
}

dependencies {
    // Crypto
    implementation("com.google.crypto.tink:tink:1.6.0")
    implementation("info.weboftrust:ld-signatures-java:0.4.0")
    implementation("decentralized-identity:jsonld-common-java:0.2.0")
    implementation("com.goterl:lazysodium-java:5.0.1")
    implementation("com.github.multiformats:java-multibase:v1.1.0")

    // Ethereum
    implementation("org.web3j:core:4.8.4")
    implementation("org.web3j:crypto:4.8.4")

    implementation("com.google.guava:guava:30.1.1-jre")

    // JSON
    implementation("org.json:json:20210307")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")

    // DB
    implementation("org.xerial:sqlite-jdbc:3.36.0.1")
    implementation("com.zaxxer:HikariCP:4.0.3")

    // CLI
    implementation("com.github.ajalt.clikt:clikt-jvm:3.2.0")
    implementation("com.github.ajalt.clikt:clikt:3.2.0")

    // Misc
    implementation("commons-io:commons-io:2.10.0")

    // HTTP
    implementation("io.ktor:ktor-client-core:1.6.1")
    implementation("io.ktor:ktor-client-cio:1.6.1")
    implementation("io.ktor:ktor-client-serialization:1.6.1")
    implementation("io.ktor:ktor-client-logging:1.6.1")

    // REST
    implementation("io.javalin:javalin-bundle:3.13.8")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.0-alpha2")
    implementation("org.apache.logging.log4j:log4j-core:2.14.1")
    implementation("org.apache.logging.log4j:log4j-api:2.14.1")
    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.14.1")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.8")

    // Config
    implementation("com.sksamuel.hoplite:hoplite-core:1.4.3")
    implementation("com.sksamuel.hoplite:hoplite-yaml:1.4.3")
    implementation("com.sksamuel.hoplite:hoplite-hikaricp:1.4.3")

    // Service-Matrix
    implementation("id.walt.servicematrix:WaltID-ServiceMatrix:1.0.1")

    // Kotlin
    implementation(kotlin("stdlib"))

    // Testing
    testImplementation(kotlin("test-junit"))

    //testImplementation("io.kotest:kotest-runner-junit5:4.6.0")
    //testImplementation("io.kotest:kotest-assertions-core:4.6.0")
}

tasks.test {
    useJUnit()
}

/*
tasks.withType<Test> {
    useJUnitPlatform()
}
 */

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "13"
}

tasks.named<CreateStartScripts>("startScripts") {
    doLast {
        windowsScript.writeText(windowsScript.readText().replace(Regex("set CLASSPATH=.*"), "set CLASSPATH=%APP_HOME%\\\\lib\\\\*"))
    }
}

val fatJar = task("fatJar", type = Jar::class) {
    group = "build"

    archiveBaseName.set("${project.name}-with-dependencies")

    manifest {
        attributes["Implementation-Title"] = "Gradle Jar Bundling"
        attributes["Implementation-Version"] = archiveVersion.get()
        attributes["Main-Class"] = "org.letstrust.MainCliKt"
    }

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

application {
    mainClass.set("org.letstrust.MainCliKt")
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

licenseReport {
    renderers = arrayOf<com.github.jk1.license.render.ReportRenderer>(com.github.jk1.license.render.InventoryHtmlReportRenderer("report.html", "Backend"))
    filters = arrayOf<com.github.jk1.license.filter.DependencyFilter>(com.github.jk1.license.filter.LicenseBundleNormalizer())
}

