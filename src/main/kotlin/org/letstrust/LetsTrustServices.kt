package org.letstrust


import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hikari.HikariDataSourceDecoder
import com.sksamuel.hoplite.yaml.YamlParser
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.LoggerConfig
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.letstrust.services.key.FileSystemKeyStore
import org.letstrust.services.key.KeyStore
import org.letstrust.services.key.SqlKeyStore
import java.io.File
import java.security.Security
import java.util.*

inline class Port(val value: Int)
inline class Host(val value: String)
enum class KeystoreType { file, database, custom }
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
    val keystore: Keystore,
    val essif: Essif?,
    val server: Server?,
    val hikariDataSource: HikariDataSource = HikariDataSource()
) {
    val log = KotlinLogging.logger {}

    init {
        log.debug { this }
    }
}

object LetsTrustServices {

    val log = KotlinLogging.logger {}

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    inline fun <reified T> load(): T {

        log.debug { "Loading: " + T::class }

        val conf = this.loadConfig()

        val service = when (T::class) {
            KeyStore::class -> loadKeyStore(conf)
            HikariDataSource::class -> conf.hikariDataSource as T
            else -> throw Exception("Service " + T::class + " not registered")
        }

        log.debug { "Service: $service loaded" }
        return service as T
    }

    fun loadKeyStore(conf: LetsTrustConfig) = when (conf.keystore.type) {
        KeystoreType.custom -> loadCustomKeyStore()
        KeystoreType.database -> SqlKeyStore
        else -> FileSystemKeyStore
    }

    private fun loadCustomKeyStore(): KeyStore {
        println("Loading Custom KeyStore")
        val loader = ServiceLoader.load(KeyStore::class.java)
        val customKeyStore = loader.iterator().next()
        println("Loaded custom KeyStore: $customKeyStore")
        return customKeyStore!!
    }

    fun loadConfig(): LetsTrustConfig = ConfigLoader.Builder()
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
