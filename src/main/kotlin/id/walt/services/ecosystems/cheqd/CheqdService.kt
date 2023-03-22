package id.walt.services.ecosystems.cheqd

import id.walt.common.KlaxonWithConverters
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.crypto.toBase64Url
import id.walt.model.Did
import id.walt.model.did.DidCheqd
import id.walt.services.crypto.CryptoService
import id.walt.services.ecosystems.cheqd.models.job.didstates.Secret
import id.walt.services.ecosystems.cheqd.models.job.didstates.SigningResponse
import id.walt.services.ecosystems.cheqd.models.job.didstates.action.ActionDidState
import id.walt.services.ecosystems.cheqd.models.job.didstates.finished.FinishedDidState
import id.walt.services.ecosystems.cheqd.models.job.request.JobCreateRequest
import id.walt.services.ecosystems.cheqd.models.job.request.JobDeactivateRequest
import id.walt.services.ecosystems.cheqd.models.job.request.JobSignRequest
import id.walt.services.ecosystems.cheqd.models.job.response.JobActionResponse
import id.walt.services.ecosystems.cheqd.models.job.response.didresponse.DidGetResponse
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.bouncycastle.util.encoders.Hex
import java.util.*

object CheqdService {

    private val log = KotlinLogging.logger { }
    private val client = HttpClient()
    private val cryptoService = CryptoService.getService()
    private val keyService = KeyService.getService()

    private const val verificationMethod = "Ed25519VerificationKey2020"
    private const val methodSpecificIdAlgo = "uuid"
    private const val registrarUrl = "https://registrar.walt.id/cheqd"
    private const val registrarApiVersion = "1.0"
    private const val didCreateUrl =
        "$registrarUrl/$registrarApiVersion/did-document?verificationMethod=%s&methodSpecificIdAlgo=%s&network=%s&publicKeyHex=%s"
    private const val didOnboardUrl = "$registrarUrl/$registrarApiVersion/create"
    private const val didDeactivateUrl = "$registrarUrl/$registrarApiVersion/deactivate"
    private const val didUpdateUrl = "$registrarUrl/$registrarApiVersion/update"

    fun createDid(keyId: String, network: String): DidCheqd = let {
        val key = keyService.load(keyId, KeyType.PRIVATE)
        if (key.algorithm != KeyAlgorithm.EdDSA_Ed25519) throw IllegalArgumentException("Key of type Ed25519 expected")
//        step#0. get public key hex
        val pubKeyHex = Hex.toHexString(key.getPublicKeyBytes())
//        step#1. fetch the did document from cheqd registrar
        val response = runBlocking {
            client.get(String.format(didCreateUrl, verificationMethod, methodSpecificIdAlgo, network, pubKeyHex)).bodyAsText()
        }
//        step#2. onboard did with cheqd registrar
        KlaxonWithConverters().parse<DidGetResponse>(response)?.let {
//            step#2a. initialize
            val job = initiateDidJob(didCreateUrl, KlaxonWithConverters().toJsonString(JobCreateRequest(it.didDoc)))
                ?: throw Exception("Failed to initialize the did onboarding process")
//            step#2b. sign the serialized payload
            val signatures = signPayload(key.keyId, job)
//            step#2c. finalize
            val didDocument = (finalizeDidJob(
                didCreateUrl,
                job.jobId,
                it.didDoc.verificationMethod.first().id, // TODO: associate verificationMethodId with signature
                signatures
            )?.didState as? FinishedDidState)?.didDocument
                ?: throw IllegalArgumentException("Failed to finalize the did onboarding process")

            Did.decode(KlaxonWithConverters().toJsonString(didDocument)) as DidCheqd
        } ?: throw IllegalArgumentException("Failed to fetch the did document from cheqd registrar helper")
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

    fun deactivateDid(did: String){
        val job = initiateDidJob(didCreateUrl, KlaxonWithConverters().toJsonString(JobDeactivateRequest(did)))
            ?: throw Exception("Failed to initialize the did onboarding process")
        val signatures = signPayload(KeyId(""), job)
        val didDocument = (finalizeDidJob(
            didDeactivateUrl,
            job.jobId,
            "", // TODO: associate verificationMethodId with signature
            signatures
        )?.didState as? FinishedDidState)?.didDocument
            ?: throw Exception("Failed to finalize the did onboarding process")
    }

    fun updateDid(did: String){
        TODO()
    }

    private fun initiateDidJob(url: String, body: String) = let{
        val response = runBlocking {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.bodyAsText()
        }
        KlaxonWithConverters().parse<JobActionResponse>(response)
    }

    private fun finalizeDidJob(url: String, jobId: String, verificationMethodId: String, signatures: List<String>) = let{
        val actionResponse = runBlocking {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(
                    KlaxonWithConverters().toJsonString(
                        JobSignRequest(
                            jobId = jobId,
                            secret = Secret(
                                signingResponse = signatures.map {
                                    SigningResponse(
                                        signature = toBase64Url(it),
                                        verificationMethodId = verificationMethodId,
                                    )
                                }
                            )
                        )
                    )
                )
            }.bodyAsText()
        }
        KlaxonWithConverters().parse<JobActionResponse>(actionResponse)
    }

    private fun signPayload(keyId: KeyId, job: JobActionResponse) = let {
        val state = (job.didState as? ActionDidState) ?: throw IllegalArgumentException("Unexpected did state")
        val payloads = state.signingRequest.map {
            Base64.getDecoder().decode(it.serializedPayload)
        }
        // TODO: sign with key having alias from verification method
        payloads.map { Base64.getUrlEncoder().encodeToString(cryptoService.sign(keyId, it)) }
    }

}
