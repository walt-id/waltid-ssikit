package id.walt.signatory.revocation.simplestatus2022

import id.walt.model.credential.status.CredentialStatus
import id.walt.model.credential.status.SimpleCredentialStatus2022
import id.walt.services.WaltIdServices
import id.walt.signatory.revocation.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class SimpleCredentialClientService: CredentialStatusClientService {

    private val logger = KotlinLogging.logger("WaltIdRevocationClientService")
    private val credentialStorage = SimpleCredentialStatus2022StorageService

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
        logger.debug { "Checking revocation at $parameter" }
        http.get(tokenParameter.revocationCheckUrl).body<TokenRevocationStatus>()
    }

    override fun revoke(parameter: RevocationConfig) {
        val baseTokenUrl = (parameter as TokenRevocationConfig).baseTokenUrl
        val baseToken = baseTokenUrl.split("/").last()
        if (baseToken.length != 72) throw IllegalArgumentException("base token has to have 72 chars (uuiduuid)")

        logger.debug { "Revoking at $baseTokenUrl" }
        credentialStorage.revokeToken(baseToken)
    }

    override fun create(parameter: CredentialStatusFactoryParameter): CredentialStatus = SimpleCredentialStatus2022(
        id = (parameter as? SimpleStatusFactoryParameter)?.id ?: ""
    )
}
