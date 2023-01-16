package id.walt.services.ecosystems.cheqd

import id.walt.common.klaxonWithConverters
import id.walt.model.did.DidCheqd
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

object CheqdService {

    private val log = KotlinLogging.logger { }

    fun createDid(keyId: String): DidCheqd {
        TODO("CHEQD DID creation is not yet implemented")
    }

    fun resolveDid(did: String): DidCheqd {
        log.debug { "Resolving did:cheqd, DID: $did" }
        val resultText = runBlocking {
            HttpClient(CIO).get("https://resolver.cheqd.net/1.0/identifiers/$did").bodyAsText()
        }

        log.debug { "Received body from CHEQD resolver: $resultText" }

        val resp = klaxonWithConverters.parse<DidCheqdResolutionResponse>(resultText)
            ?: throw IllegalArgumentException("Could not decode did:cheqd resolve response!")

        log.debug { "Decoded response from CHEQD resolver: $resp" }

        if (resp.didResolutionMetadata.error != null)
            throw IllegalArgumentException("Could not resolve did:cheqd, resolver responded: ${resp.didResolutionMetadata.error}")

        resp.didDocument ?: throw IllegalArgumentException("Response for did:cheqd did not contain a DID document!")

        log.debug { "Found DID document in CHEQD resolver response: ${resp.didDocument}" }

        return resp.didDocument!! // cheqd above for null
    }

}

fun main() {
    println(CheqdService.resolveDid("did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY"))
}
