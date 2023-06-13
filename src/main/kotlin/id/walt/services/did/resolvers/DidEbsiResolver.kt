package id.walt.services.did.resolvers

import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.did.DidEbsi
import id.walt.services.did.DidEbsiResolveOptions
import id.walt.services.did.DidOptions
import id.walt.services.did.composers.DidDocumentComposer
import id.walt.services.did.composers.models.DocumentComposerJwkParameter
import id.walt.services.ecosystems.essif.TrustedIssuerClient
import id.walt.services.key.KeyService
import io.ipfs.multibase.Multibase
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class DidEbsiResolver(
    private val httpClient: HttpClient,
    private val keyService: KeyService,
    private val ebsiV2documentComposer: DidDocumentComposer<DidEbsi>,
) : DidResolverBase<DidEbsi>() {

    private val didRegistryPath = "did-registry/${TrustedIssuerClient.apiVersion}/identifiers"
    override fun resolve(didUrl: DidUrl, options: DidOptions?) = (options as? DidEbsiResolveOptions)?.takeIf {
        it.isRaw
    }?.let { resolveDidEbsiRaw(didUrl.did) }?:resolveEbsi(didUrl)

    private fun resolveEbsi(didUrl: DidUrl) = when (Multibase.decode(didUrl.identifier).first().toInt()) {
        1 -> resolveDidEbsiV1(didUrl)
        2 -> resolveDidEbsiV2(didUrl)
        else -> throw IllegalArgumentException("did:ebsi must have version 1 or 2")
    }

    private fun resolveDidEbsiV1(didUrl: DidUrl): DidEbsi = runBlocking {

        log.debug { "Resolving DID ${didUrl.did}..." }

        var didDoc: String
        var lastEx: ClientRequestException? = null

        for (i in 1..5) {
            try {
                log.debug { "Resolving did:ebsi at: ${TrustedIssuerClient.domain}/$didRegistryPath/${didUrl.did}" }
                didDoc = httpClient.get("${TrustedIssuerClient.domain}/$didRegistryPath/${didUrl.did}").bodyAsText()
                log.debug { "Result: $didDoc" }
                return@runBlocking Did.decode(didDoc)!! as DidEbsi
            } catch (e: ClientRequestException) {
                log.debug { "Resolving did ebsi failed: fail $i" }
                delay(1000)
                lastEx = e
            }
        }
        log.debug { "Could not resolve did ebsi!" }
        throw lastEx ?: Exception("Could not resolve did ebsi!")
    }

    private fun resolveDidEbsiV2(didUrl: DidUrl): DidEbsi {
        val jwk = keyService.toJwk(didUrl.did)
        if (DidUrl.generateDidEbsiV2DidUrl(jwk.computeThumbprint().decode()).identifier != didUrl.identifier) {
            throw IllegalArgumentException("Public key doesn't match with DID identifier")
        }
        return ebsiV2documentComposer.make(DocumentComposerJwkParameter(didUrl, jwk))
    }

    private fun resolveDidEbsiRaw(did: String): Did = runBlocking {
        log.debug { "Resolving DID $did" }
        val didDoc = httpClient.get("${TrustedIssuerClient.domain}/$didRegistryPath/$did").bodyAsText()
        log.debug { didDoc }
        Did.decode(didDoc) ?: throw Exception("Could not resolve $did")
    }
}
