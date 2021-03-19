package org.letstrust


import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.zaxxer.hikari.HikariDataSource
import org.letstrust.services.key.FileSystemKeyStore
import org.letstrust.services.key.KeyStore
import org.letstrust.services.key.SqlKeyStore
import java.io.File
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
    val hikariDataSource: HikariDataSource = HikariDataSource()) {
    init {
        println(this)
    }
}

object LetsTrustServices {

    fun <T> load(serviceName: String): T {

        val conf = loadConfig()

        if (serviceName == KeyStore::javaClass.name) {
            val keyStore = when (conf.keystore.type) {
                KeystoreType.custom -> loadCustomKeyStore()
                KeystoreType.database -> SqlKeyStore
                else -> FileSystemKeyStore
            }
            println("KeyStore loaded: $keyStore")
            return keyStore as T
        } else {
            throw Exception("Service $serviceName not registered")
        }
    }

    private fun loadCustomKeyStore(): KeyStore {
        println("Loading Custom KeyStore")
        val loader = ServiceLoader.load(KeyStore::class.java)
        val customKeyStore = loader.iterator().next()
        println("Loaded custom KeyStore: $customKeyStore")
        return customKeyStore!!
    }

    private fun loadConfig() = ConfigLoader.Builder()
        .addSource(PropertySource.file(File("letstrust.yaml"), optional = true))
        .addSource(PropertySource.resource("/letstrust-default.yaml"))
        .build()
        .loadConfigOrThrow<LetsTrustConfig>()

}
