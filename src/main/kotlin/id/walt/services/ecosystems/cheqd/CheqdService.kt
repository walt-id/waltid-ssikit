package id.walt.services.ecosystems.cheqd

import id.walt.common.KlaxonWithConverters
import id.walt.crypto.toPEM
import id.walt.model.Did
import id.walt.model.did.DidCheqd
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.ecosystems.cheqd.models.job.didstates.Secret
import id.walt.services.ecosystems.cheqd.models.job.didstates.SigningResponse
import id.walt.services.ecosystems.cheqd.models.job.didstates.action.ActionDidState
import id.walt.services.ecosystems.cheqd.models.job.didstates.finished.FinishedDidState
import id.walt.services.ecosystems.cheqd.models.job.request.JobActionRequest
import id.walt.services.ecosystems.cheqd.models.job.request.JobSignRequest
import id.walt.services.ecosystems.cheqd.models.job.response.JobActionResponse
import id.walt.services.ecosystems.cheqd.models.job.response.didresponse.DidDocObject
import id.walt.services.ecosystems.cheqd.models.job.response.didresponse.DidGetResponse
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.bouncycastle.crypto.Signer
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.security.Signature
import java.util.*

object CheqdService {

    private val log = KotlinLogging.logger { }
    private val client = HttpClient()

    private const val verificationMethod = "Ed25519VerificationKey2020"
    private const val methodSpecificIdAlgo = "uuid"
    private const val network = "testnet"
    private const val didCreateUrl = "https://did-registrar.cheqd.net/1.0/did-document?verificationMethod=%s&methodSpecificIdAlgo=%s&network=%s&publicKeyHex=%s"
    private const val didOnboardUrl = "https://did-registrar.cheqd.net/1.0/create"

    fun createDid(keyId: String): DidCheqd  = let{
        val key = KeyService.getService().load(keyId, KeyType.PRIVATE)
//        step#0. get public key hex
        val pubKeyHex = Hex.toHexString(key.getPublicKeyBytes())
//        step#1. fetch the did document from cheqd registrar
        val response = runBlocking {
            client.get(String.format(didCreateUrl, verificationMethod, methodSpecificIdAlgo, network, pubKeyHex)).bodyAsText()
        }
//        step#2. onboard did with cheqd registrar
        KlaxonWithConverters().parse<DidGetResponse>(response)?.let {
//            step#2a. initialize
            val job = initiateDidOnboarding(it.didDoc) ?: throw Exception("Failed to initialize the did onboarding process")
            val state = (job.didState as? ActionDidState) ?: throw IllegalArgumentException("Unexpected did state")
//            step#2b. sign the serialized payload
            val signature = signPayload(key.keyPair!!.private, state.signingRequest.firstOrNull()?.serializedPayload ?: "")
//            step#2c. finalize
            val didDocument = (finalizeDidOnboarding(job.jobId, it.didDoc.verificationMethod.first().id, signature)?.didState as? FinishedDidState)?.didDocument
                ?: throw Exception("Failed to finalize the did onboarding process")
            Did.decode(KlaxonWithConverters().toJsonString(didDocument)) as DidCheqd
        }?: throw Exception("Failed to fetch the did document from cheqd registrar helper")
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

    private fun initiateDidOnboarding(didDocObject: DidDocObject) = let {
        val actionResponse = runBlocking {
            client.post(didOnboardUrl) {
                contentType(ContentType.Application.Json)
                setBody(KlaxonWithConverters().toJsonString(JobActionRequest(didDocObject)))
            }.bodyAsText()
        }
        KlaxonWithConverters().parse<JobActionResponse>(actionResponse)
    }

    private fun signPayload(privateKey: PrivateKey, payload: String) = let {
//        prepare signature
        val signature: Signature = Signature.getInstance("Ed25519")
        signature.initSign(privateKey)
        signature.update(payload.toByteArray(StandardCharsets.UTF_8))
        val signResult: ByteArray = signature.sign()
//        prepare result
        Base64.getEncoder().encodeToString(signResult)
    }

    private fun finalizeDidOnboarding(jobId: String, verificationMethodId: String, signature: String) = let{
        val actionResponse = runBlocking {
            client.post(didOnboardUrl) {
                contentType(ContentType.Application.Json)
                setBody(KlaxonWithConverters().toJsonString(JobSignRequest(
                    jobId = jobId,
                    secret = Secret(
                        signingResponse = listOf(SigningResponse(
                            signature = signature,
                            verificationMethodId = verificationMethodId,
                        ))
                    )
                )))
            }.bodyAsText()
        }
        KlaxonWithConverters().parse<JobActionResponse>(actionResponse)
    }

}

fun main() {
    println(CheqdService.resolveDid("did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY"))
}
