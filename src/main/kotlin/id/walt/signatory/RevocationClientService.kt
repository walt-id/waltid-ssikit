package id.walt.signatory

import com.fasterxml.jackson.databind.json.JsonMapper
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.WaltIdServices
import id.walt.signatory.RevocationService.RevocationResult
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.util.*

open class RevocationClientService : WaltIdService() {
    override val implementation get() = serviceImplementation<RevocationClientService>()

    open fun checkRevoked(revocationCheckUrl: String): RevocationResult =
        implementation.checkRevoked(revocationCheckUrl)

    open fun revoke(baseTokenUrl: String): Unit = implementation.revoke(baseTokenUrl)

    open fun createBaseToken(): String = implementation.createBaseToken()
    open fun deriveRevocationToken(baseToken: String): String = implementation.deriveRevocationToken(baseToken)

    companion object : ServiceProvider {
        override fun getService() = object : RevocationClientService() {}
        override fun defaultImplementation() = WaltIdRevocationClientService()
    }
}

class WaltIdRevocationClientService : RevocationClientService() {

    private val logger = KotlinLogging.logger("WaltIdRevocationClientService")

    private val http = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer(
                jackson = JsonMapper.builder()
                    .findAndAddModules()
                    .build()
            ).apply {

            }
        }
        if (WaltIdServices.httpLogging) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.HEADERS
            }
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }

    override fun checkRevoked(revocationCheckUrl: String): RevocationResult = runBlocking {
        val token = revocationCheckUrl.split("/").last()
        if (token.contains("-")) throw IllegalArgumentException("Revocation token contains '-', you probably didn't supply a derived revocation token, but a base token.")

        logger.debug { "Checking revocation at $revocationCheckUrl" }
        http.get(revocationCheckUrl)
    }

    override fun revoke(baseTokenUrl: String) {
        val baseToken = baseTokenUrl.split("/").last()
        if (baseToken.length != 72) throw IllegalArgumentException("base token has to have 72 chars (uuiduuid)")

        logger.debug { "Revoking at $baseTokenUrl" }
        runBlocking {
            http.post<Unit>(baseTokenUrl)
        }
    }

    override fun createBaseToken() = UUID.randomUUID().toString() + UUID.randomUUID().toString()
    override fun deriveRevocationToken(baseToken: String): String = RevocationService.getRevocationToken(baseToken)
}
