package id.walt.services.ecosystems.cheqd

import id.walt.common.KlaxonWithConverters
import id.walt.model.Did
import id.walt.model.did.DidCheqd
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.bouncycastle.util.encoders.Hex

object CheqdService {

    private val log = KotlinLogging.logger { }
    private val client = HttpClient()

    private const val verificationMethod = "Ed25519VerificationKey2020"
    private const val methodSpecificIdAlgo = "uuid"
    private const val network = "testnet"
    private const val didCreateUrl = "https://did-registrar.cheqd.net/1.0/did-document?verificationMethod=%s&methodSpecificIdAlgo=%s&network=%s&publicKeyHex=%s"

    fun createDid(keyId: String): DidCheqd {
        val key = KeyService.getService().load(keyId, KeyType.PRIVATE)
        val pubKeyHex = Hex.toHexString(key.getPublicKeyBytes())
        val response = runBlocking {
            client.get(String.format(didCreateUrl, verificationMethod, methodSpecificIdAlgo, network, pubKeyHex)).bodyAsText()
        }
        Did.decode(response)
        TODO("CHEQD DID creation is not yet implemented")
    }

    fun resolveDid(did: String): DidCheqd {
        log.debug { "Resolving did:cheqd, DID: $did" }
        val resultText = runBlocking {
            client.get("https://resolver.cheqd.net/1.0/identifiers/$did").bodyAsText()
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

    private fun onboardDid(did: String){

    }

}

fun main() {
    println(CheqdService.resolveDid("did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY"))
}
