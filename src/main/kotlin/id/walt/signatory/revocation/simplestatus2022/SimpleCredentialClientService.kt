package id.walt.signatory.revocation.simplestatus2022

import com.beust.klaxon.Klaxon
import id.walt.services.WaltIdServices
import id.walt.signatory.revocation.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class SimpleCredentialClientService: RevocationClientService {

    private val logger = KotlinLogging.logger("WaltIdRevocationClientService")

    private val http = HttpClient {
        install(ContentNegotiation) {
            json()
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

    override fun checkRevocation(parameter: RevocationCheckParameter): RevocationStatus = runBlocking {
        val tokenParameter = parameter as TokenRevocationCheckParameter
        val token = tokenParameter.revocationCheckUrl.split("/").last()
        if (token.contains("-")) throw IllegalArgumentException("Revocation token contains '-', you probably didn't supply a derived revocation token, but a base token.")

        logger.debug { "Checking revocation at $parameter" }
        http.get(tokenParameter.revocationCheckUrl).body<TokenRevocationStatus>()
    }

    override fun revoke(parameter: RevocationConfig) {
        val baseTokenUrl = (parameter as TokenRevocationConfig).baseTokenUrl
        val baseToken = baseTokenUrl.split("/").last()
        if (baseToken.length != 72) throw IllegalArgumentException("base token has to have 72 chars (uuiduuid)")

        logger.debug { "Revoking at $baseTokenUrl" }
        runBlocking {
            http.post(baseTokenUrl)
        }
    }
}
