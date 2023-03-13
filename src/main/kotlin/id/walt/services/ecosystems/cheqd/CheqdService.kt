package id.walt.services.ecosystems.cheqd

import id.walt.common.KlaxonWithConverters
import id.walt.crypto.encBase64
import id.walt.crypto.toBase64Url
import id.walt.crypto.toPEM
import id.walt.model.Did
import id.walt.model.did.DidCheqd
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
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
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
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

    private const val verificationMethod = "Ed25519VerificationKey2018"
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
            val payload = state.signingRequest.firstOrNull()?.serializedPayload ?: throw NoSuchElementException("No serializedPayload in first-or-null signing request")
            val signature = signPayload(key.keyPair!!.private, payload)
//            val signature = CryptoService.getService().sign(key.keyId, payload.toByteArray(StandardCharsets.UTF_8))
//            step#2c. finalize
            val didDocument = (finalizeDidOnboarding(job.jobId, it.didDoc.verificationMethod.first().id, toBase64Url(signature))?.didState as? FinishedDidState)?.didDocument
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

    fun signPayload(privateKey: PrivateKey, payload: String) = let {
        val message: ByteArray = payload.toByteArray(StandardCharsets.UTF_8)
        val secretKeyParameters = Ed25519PrivateKeyParameters(privateKey.encoded, 0)
        val signer: Signer = Ed25519Signer()
        signer.init(true, secretKeyParameters)
        signer.update(message, 0, message.size)
        val signature: ByteArray = signer.generateSignature()
        Base64.getEncoder().encodeToString(signature)

////        prepare signature
//        val signature: Signature = Signature.getInstance("Ed25519")
//        signature.initSign(privateKey)
//        signature.update(payload.toByteArray(StandardCharsets.UTF_8))
//        val signResult: ByteArray = signature.sign()
////        prepare result
//        Base64.getEncoder().encodeToString(signResult)
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
//    println(CheqdService.resolveDid("did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY"))
    ServiceMatrix("service-matrix.properties")
    CheqdService.createDid("1bffc3adf53c4592bfab93ac27d074e7")

//    val payload = "EjZkaWQ6Y2hlcWQ6dGVzdG5ldDo4YWQ2ZjJhZS02MGE2LTQwZTctYTlkYi02MzBmZWM2ZTdlNTMaNmRpZDpjaGVxZDp0ZXN0bmV0OjhhZDZmMmFlLTYwYTYtNDBlNy1hOWRiLTYzMGZlYzZlN2U1MyLEAQo8ZGlkOmNoZXFkOnRlc3RuZXQ6OGFkNmYyYWUtNjBhNi00MGU3LWE5ZGItNjMwZmVjNmU3ZTUzI2tleS0xEhpFZDI1NTE5VmVyaWZpY2F0aW9uS2V5MjAyMBo2ZGlkOmNoZXFkOnRlc3RuZXQ6OGFkNmYyYWUtNjBhNi00MGU3LWE5ZGItNjMwZmVjNmU3ZTUzIjB6Nk1rdldzOE5LNlBwaGVkbWl4RHRyYVJXZTZhZkc2UVlZTUpZc2VuNDZTNzJKWkUqPGRpZDpjaGVxZDp0ZXN0bmV0OjhhZDZmMmFlLTYwYTYtNDBlNy1hOWRiLTYzMGZlYzZlN2U1MyNrZXktMWIkYzM0ODkyMTYtNGU3My00YThjLTgzMzctYjkyYmFmZDBjNDhh"
//    val key = KeyService.getService().load("1bffc3adf53c4592bfab93ac27d074e7", KeyType.PRIVATE)
////        step#0. get public key hex
//    val pubKeyHex = Hex.toHexString(key.getPublicKeyBytes())
//    println(CheqdService.signPayload(key.keyPair!!.private, payload))
//    println(pubKeyHex)

    "E_d7NFQTEy6yD-Lfw1oT7jwuK2OnEIcOh_QRBvxQqxUYhAzoqpCo_QCC1PdZ6Wx5DO8AIwbwuaN13bx5VMe6BA"
    "05vKQcePyjrJYWakruQtarNHyyGNTPBfwvQxw07QBGQIl4CkucumO3xMJik-JcsRyfalOvs6oRXJp12wbfAPCg"
    "5qKd6FcCDUa35DDkZbwYEFjiw1sGFSFUB8R-4JHvQs3oH-AiszYTQsKPLTDWk4R6VBQFuFs0LBk1wOozW5__Cg"
    "NVYw7ysqnRlWE7kuS8h40tpvRE_2qD0wUGVNdEFtbGme9wWQCcpcNDNWlDd0mZJdFTo5tLTBNXG6F2LoaDzCCw"
}
