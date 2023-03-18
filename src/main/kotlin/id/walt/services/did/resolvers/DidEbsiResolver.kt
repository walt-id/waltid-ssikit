package id.walt.services.did.resolvers

import com.beust.klaxon.Klaxon
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.Jwk
import id.walt.model.VerificationMethod
import id.walt.model.did.DidEbsi
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
) : DidResolverBase<DidEbsi>() {

    override fun resolve(didUrl: DidUrl) = when (Multibase.decode(didUrl.identifier).first().toInt()) {
        1 -> resolveDidEbsiV1(didUrl)
        2 -> resolveDidEbsiV2(didUrl)
        else -> throw Exception("did:ebsi must have version 1 or 2")
    }

    private fun resolveDidEbsiV1(didUrl: DidUrl): DidEbsi = runBlocking {

        log.debug { "Resolving DID ${didUrl.did}..." }

        var didDoc: String
        var lastEx: ClientRequestException? = null

        for (i in 1..5) {
            try {
                log.debug { "Resolving did:ebsi at: https://api-pilot.ebsi.eu/did-registry/v3/identifiers/${didUrl.did}" }
                didDoc = httpClient.get("https://api-pilot.ebsi.eu/did-registry/v3/identifiers/${didUrl.did}")
                    .bodyAsText()
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
        val vmId = "${didUrl.did}#${jwk.computeThumbprint()}"
        if (DidUrl.generateDidEbsiV2DidUrl(jwk.computeThumbprint().decode()).identifier != didUrl.identifier) {
            throw Exception("Public key doesn't match with DID identifier")
        }
        return DidEbsi(
            context = listOf("https://w3id.org/did/v1"),
            id = didUrl.did,
            verificationMethod = listOf(
                VerificationMethod(
                    id = vmId,
                    type = "JsonWebKey2020",
                    controller = didUrl.did,
                    publicKeyJwk = Klaxon().parse<Jwk>(jwk.toJSONString())
                )
            ),
            authentication = listOf(VerificationMethod.Reference(vmId)),
            assertionMethod = listOf(VerificationMethod.Reference(vmId))
        )
    }
}
