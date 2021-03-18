package org.letstrust

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.zaxxer.hikari.HikariDataSource
import org.letstrust.services.key.KeyStore
import java.io.File
import java.util.*

inline class Port(val value: Int)
inline class Host(val value: String)
data class Database(val host: Host, val port: Port, val user: String, val pass: String)
enum class KeystoreType { file, database }
data class Keystore(val type: KeystoreType)
data class Server(val host: Host, val port: Port)

// essif:
//  essifApiBaseUrl: "https://api.ebsi.xyz"
//  authorizationApi: "/authorization/v1"
//  ledgerAPI: "/ledger/v1"
//  trustedIssuerRegistryApi: "/tir/v2"
//  trustedAccreditationOrganizationRegistryApi: "/taor/v1"
//  revocationRegistry: "/revocation/v1"
//  schemaRegistry: "/revocation/v1"
data class Essif(
    val essifApiBaseUrl: String,
    val authorizationApi: String,
    val ledgerAPI: String,
    val trustedIssuerRegistryApi: String,
    val trustedAccreditationOrganizationRegistryApi: String,
    val revocationRegistry: String,
    val schemaRegistry: String
)

data class LetsTrustConfig(val env: String = "dev", val keystore: Keystore, val essif: Essif?, val server: Server?, val hikariDataSource: HikariDataSource = HikariDataSource()) {
    init {
        println(this)
    }
}

//object ServerSpec : ConfigSpec() {
//    val host by optional("0.0.0.0")
//    val tcpPort by required<Int>()
//}
//
//data class ConfigTestReport(val db: Map<String, String>)
//data class Db(val driverClassName: String, val url: String)
//
//
//
//object EssifSpec : ConfigSpec("essif") {
//    val baseURL by required<String>()
//}

//data class Config(val name: String,
//                  val env: String,
//                  val host: String,
//                  val port: Int,
//                  val user: String,
//                  val password: Masked
//)

fun main() {

    //  val conf =ConfigLoader().loadConfig<MyConfig>("/application.yaml")

//    .addSource(
//        YamlPropertySource(
//            """
//        asdf: "localhost"
//        qwer: 1234
//     """
//        )
//    )
    val loader = ServiceLoader.load(KeyStore::class.java)
    val ks = loader.iterator().next()

    println(ks)

    val conf = ConfigLoader.Builder()
        .addSource(PropertySource.file(File("letstrust.yaml"), optional = true))
        .addSource(PropertySource.resource("/letstrust-default.yaml"))
        .build()
        .loadConfigOrThrow<LetsTrustConfig>()

//    val yamlContent = """
//db:
//  driverClassName: org.h2.Driver
//  url: 'jdbc:h2:mem:db;DB_CLOSE_DELAY=-1'
//    """.trimIndent()
//
//    val config = Config {
//        addSpec(ServerSpec)
//        addSpec(EssifSpec)
//        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//    }
//   //     .from.yaml.string(yamlContent)
//        .from.yaml.file("dev.yml", true)
//        .from.systemProperties()
//        .from.env()
//    val db = config.toValue<ConfigTestReport>()
//    println(db)
//    val conf = Config()
//        .from.yaml.string(yamlContent)
//        .from.yaml.file("dev.yml", true)
//        .from.json.resource("server.json", true)
//        .from.env()
//        .from.systemProperties()
//
//    val db = conf.toValue<ConfigTestReport>()
//
//    println(db)
//
//    val server = config
//        .at("server")
//        .toValue<Server>()
//    server.start()
//    println(server)
}
