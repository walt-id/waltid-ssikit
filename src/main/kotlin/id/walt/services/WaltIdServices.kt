package id.walt.services

import com.google.crypto.tink.config.TinkConfig
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hikari.HikariDataSourceDecoder
import com.sksamuel.hoplite.yaml.YamlParser
import com.zaxxer.hikari.HikariDataSource
import id.walt.Values
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.Security

enum class CryptoProvider { SUN, TINK, CUSTOM }

data class WaltIdConfig(
    val hikariDataSource: HikariDataSource = HikariDataSource()
)

object WaltIdServices {

    const val dataDir = "data"
    const val keyDir = "$dataDir/key/"
    const val ebsiDir = "$dataDir/ebsi/"
    const val revocationDir = "$dataDir/revocation"

    val httpLogging = false
    private val log = KotlinLogging.logger {}
    private val bearerTokenStorage = mutableListOf<BearerTokens>()

    //val http = HttpClient(CIO) {
    val httpNoAuth = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        if (httpLogging) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.HEADERS
            }
        }
    }

    val httpWithAuth = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(io.ktor.client.plugins.auth.Auth) {
            bearer {
                loadTokens {
                    bearerTokenStorage.last()
                }
            }
        }
        if (httpLogging) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.HEADERS
            }
        }
    }

    init {
        val javaVersion = System.getProperty("java.runtime.version")
        println("walt.id SSI Kit ${Values.version} (running on Java $javaVersion)")

        if (Runtime.version().feature() < 16) {
            log.error { "Java version 16+ is required!" }
        }

        // BC is required for
        // - secp256k1 curve
        Security.addProvider(BouncyCastleProvider())

        TinkConfig.register()

        createDirStructure()

        println()
    }

    fun loadHikariDataSource(): HikariDataSource {
        val conf = loadConfig()
        return conf.hikariDataSource
    }

    fun createDirStructure() {
        log.debug { "Creating dir-structure at: $dataDir" }
        Files.createDirectories(Path.of(keyDir))
        Files.createDirectories(Path.of("$dataDir/did/created"))
        Files.createDirectories(Path.of("$dataDir/did/resolved"))
        Files.createDirectories(Path.of("$dataDir/vc/templates"))
        Files.createDirectories(Path.of("$dataDir/vc/created"))
        Files.createDirectories(Path.of("$dataDir/vc/presented"))
        Files.createDirectories(Path.of(ebsiDir))
        Files.createDirectories(Path.of(revocationDir))
    }

    fun loadConfig() = ConfigLoader.builder()
        .addFileExtensionMapping("yaml", YamlParser())
        .addSource(PropertySource.file(File("walt.yaml"), optional = true))
        .addSource(PropertySource.resource("/walt-default.yaml"))
        .addDecoder(HikariDataSourceDecoder())
        .build()
        .loadConfigOrThrow<WaltIdConfig>()

    fun addBearerToken(token: String) = bearerTokenStorage.add(BearerTokens(token, token))

    fun clearBearerTokens() = bearerTokenStorage.clear()

    suspend inline fun <T> callWithToken(token: String, vararg arg: T, callback: (Any) -> HttpResponse): Result<String> {
        addBearerToken(token)
        val result = callback(arg)
        clearBearerTokens()
        return when (result.status) {
            HttpStatusCode.Accepted,
            HttpStatusCode.Created,
            HttpStatusCode.OK -> Result.success(result.bodyAsText())
            else -> Result.failure(Exception(result.bodyAsText()))

        }
    }

    fun shutdown() {
        log.debug { "Shutting down ${this.javaClass.simpleName}..." }
        closeResource(httpNoAuth)
        closeResource(httpWithAuth)
    }

    private fun closeResource(resource: Closeable) = runCatching {
        resource.close()
    }
}
