package id.walt.services.did.resolvers

import id.walt.common.KlaxonWithConverters
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.did.DidCheqd
import id.walt.services.did.DidOptions
import id.walt.services.ecosystems.cheqd.DidCheqdResolutionResponse
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

class DidCheqdResolver(private val httpClient: HttpClient) : DidResolverBase<DidCheqd>() {
    override fun resolve(didUrl: DidUrl, options: DidOptions?): Did {
        log.debug { "Resolving did:cheqd, DID: ${didUrl.did}" }
        val resultText = runBlocking {
            httpClient.get("https://resolver.cheqd.net/1.0/identifiers/${didUrl.did}"){
                headers{
                    append("contentType", "application/did+ld+json")
                }
            }.bodyAsText()
        }

        log.debug { "Received body from CHEQD resolver: $resultText" }

        val resp = KlaxonWithConverters().parse<DidCheqdResolutionResponse>(resultText)
            ?: throw IllegalArgumentException("Could not decode did:cheqd resolve response!")

        log.debug { "Decoded response from CHEQD resolver: $resp" }

        if (resp.didResolutionMetadata.error != null)
            throw IllegalArgumentException("Could not resolve did:cheqd, resolver responded: ${resp.didResolutionMetadata.error}")

        resp.didDocument ?: throw IllegalArgumentException("Response for did:cheqd did not contain a DID document!")

        log.debug { "Found DID document in CHEQD resolver response: ${resp.didDocument}" }

        return resp.didDocument!! // cheqd above for null
    }
}
