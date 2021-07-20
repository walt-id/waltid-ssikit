package org.letstrust


import com.google.crypto.tink.config.TinkConfig
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hikari.HikariDataSourceDecoder
import com.sksamuel.hoplite.yaml.YamlParser
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.LoggerConfig
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.Security

enum class CryptoProvider { SUN, TINK, CUSTOM }
inline class Port(val value: Int)
inline class Host(val value: String)
enum class KeystoreType { FILE, DATABASE, TINK, AZURE_KEY_VAULT, CUSTOM }
data class Keystore(val type: KeystoreType)
data class Server(val host: Host, val port: Port)

data class Essif(
    val essifApiBaseUrl: String,
    val authorizationApi: String,
    val ledgerAPI: String,
    val trustedIssuerRegistryApi: String,
    val trustedAccreditationOrganizationRegistryApi: String,
    val revocationRegistry: String,
    val schemaRegistry: String
)

data class LetsTrustConfig(
    val env: String = "dev",
    val essif: Essif?,
    val server: Server?,
    val cryptoProvider: CryptoProvider = CryptoProvider.SUN,
    val hikariDataSource: HikariDataSource = HikariDataSource()
) {
    val log = KotlinLogging.logger {}

    init {
        log.debug { this }
    }
}

object LetsTrustServices {

    const val dataDir = "data"
    const val keyDir = "$dataDir/key/"
    const val ebsiDir = "$dataDir/ebsi/"

    val httpLogging = false
    val log = KotlinLogging.logger {}

    val http = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        if (httpLogging) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.HEADERS
            }
        }
    }

    init {
        /*val javaVersion = System.getProperty("java.runtime.version")
        println("Let's Trust SSI Core ${Values.version} (running on Java $javaVersion)")

        if (Runtime.version().feature() < 15) {
            log.error { "Java version 15+ is required!" }
        }*/

        // BC is required for
        // - secp256k1 curve
        Security.addProvider(BouncyCastleProvider())

        TinkConfig.register()

        println()
    }

    fun loadHikariDataSource(): HikariDataSource {
        val conf = this.loadConfig()
        return conf.hikariDataSource
    }

    fun createDirStructure() {
        log.debug { "Creating dir-structure at: ${dataDir}" }
        Files.createDirectories(Path.of(keyDir))
        Files.createDirectories(Path.of("${dataDir}/did/created"))
        Files.createDirectories(Path.of("${dataDir}/did/resolved"))
        Files.createDirectories(Path.of("${dataDir}/vc/templates"))
        Files.createDirectories(Path.of("${dataDir}/vc/created"))
        Files.createDirectories(Path.of("${dataDir}/vc/presented"))
        Files.createDirectories(Path.of("${ebsiDir}"))
    }

    fun loadConfig() = ConfigLoader.Builder()
        .addFileExtensionMapping("yaml", YamlParser())
        .addSource(PropertySource.file(File("letstrust.yaml"), optional = true))
        .addSource(PropertySource.resource("/letstrust-default.yaml"))
        .addDecoder(HikariDataSourceDecoder())
        .build()
        .loadConfigOrThrow<LetsTrustConfig>()

    fun setLogLevel(level: Level) {
        val ctx: LoggerContext = LogManager.getContext(false) as LoggerContext
        val logConfig: LoggerConfig = ctx.configuration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME)
        logConfig.level = level
        ctx.updateLoggers()
        log.debug { "Set log-level to $level" }
    }

}
