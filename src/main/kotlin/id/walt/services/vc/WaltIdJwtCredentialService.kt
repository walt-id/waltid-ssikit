package id.walt.services.vc

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.walt.auditor.VerificationPolicyResult
import id.walt.credentials.w3c.*
import id.walt.credentials.w3c.schema.SchemaValidatorFactory
import id.walt.sdjwt.SDField
import id.walt.sdjwt.SDJwt
import id.walt.sdjwt.SDPayload
import id.walt.sdjwt.toSDMap
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

private val log = KotlinLogging.logger {}

private const val JWT_VC_CLAIM = "vc"
private const val JWT_VP_CLAIM = "vp"

open class WaltIdJwtCredentialService : JwtCredentialService() {

    private val jwtService = JwtService.getService()

    override fun sign(jsonCred: String, config: ProofConfig): String {
        log.debug { "Signing JWT object with config: $config" }

        val crd = jsonCred.toVerifiableCredential()
        val issuerDid = config.issuerDid
        val issueDate = config.issueDate ?: Instant.now()
        val validDate = config.validDate ?: Instant.now()
        val jwtClaimsSet = JWTClaimsSet.Builder()
            .jwtID(config.credentialId)
            .issuer(issuerDid)
            .subject(config.subjectDid)
            .issueTime(Date.from(issueDate))
            .notBeforeTime(Date.from(validDate))

        if (config.expirationDate != null)
            jwtClaimsSet.expirationTime(Date.from(config.expirationDate))

        config.verifierDid?.let { jwtClaimsSet.audience(config.verifierDid) }
        config.nonce?.let { jwtClaimsSet.claim("nonce", config.nonce) }

        val vcClaim = when (crd) {
            is VerifiablePresentation -> JWT_VP_CLAIM
            else -> JWT_VC_CLAIM
        }

        jwtClaimsSet.claim(vcClaim, JsonConverter.fromJsonElement(crd.toJsonObject()))

        val payload = Json.parseToJsonElement(jwtClaimsSet.build().toString()).jsonObject
        log.debug { "Signing: $payload" }

        val vm = config.issuerVerificationMethod ?: issuerDid
        val sdPayload = SDPayload.createSDPayload(payload, mapOf(
            vcClaim to SDField(false, config.selectiveDisclosure)
        ).toSDMap())
        return SDJwt.sign(sdPayload, jwtService, vm).toString()
    }

    override fun verifyVc(issuerDid: String, vc: String): Boolean {
        log.debug { "Verifying vc: $vc with issuerDid: $issuerDid" }
        return SignedJWT.parse(vc).header.keyID == issuerDid && verifyVc(vc)
    }

    override fun verify(vcOrVp: String): VerificationResult =
        when (vcOrVp.toVerifiableCredential()) {
            is VerifiablePresentation -> VerificationResult(verifyVp(vcOrVp), VerificationType.VERIFIABLE_PRESENTATION)
            else -> VerificationResult(verifyVc(vcOrVp), VerificationType.VERIFIABLE_CREDENTIAL)
        }

    override fun verifyVc(vc: String): Boolean {
        log.debug { "Verifying vc: $vc" }
        return SDJwt.parse(vc).verify(jwtService).verified
    }

    override fun verifyVp(vp: String): Boolean =
        verifyVc(vp)

    override fun present(
        vcs: List<PresentableCredential>,
        holderDid: String,
        verifierDid: String?,
        challenge: String?,
        expirationDate: Instant?
    ): String {
        log.debug { "Creating a presentation for VCs:\n$vcs" }

        val id = "urn:uuid:${UUID.randomUUID()}"
        val config = ProofConfig(
            issuerDid = holderDid,
            subjectDid = holderDid,
            verifierDid = verifierDid,
            proofType = ProofType.JWT,
            nonce = challenge,
            credentialId = id,
            expirationDate = expirationDate,
            issuerVerificationMethod = DidService.getAuthenticationMethods(holderDid)!!.first().id
        )
        val vpReqStr = VerifiablePresentationBuilder().setId(id).setHolder(holderDid)
            .setVerifiableCredentials(vcs).build().toJson()

        log.trace { "VP request: $vpReqStr" }
        log.trace { "Proof config: $$config" }

        val vp = sign(vpReqStr, config)

        log.debug { "VP created:$vp" }
        return vp
    }

    override fun listVCs(): List<String> =
        Files.walk(Path.of("data/vc/created"))
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".json") }
            .map { it.fileName.toString() }.toList()

    override fun validateSchema(vc: VerifiableCredential, schemaURI: URI): VerificationPolicyResult =
        SchemaValidatorFactory.get(schemaURI).validate(vc.toJson())

    override fun validateSchemaTsr(vc: String) = try {
        vc.toVerifiableCredential().let {
            if (it is VerifiablePresentation) return VerificationPolicyResult.success()
            val credentialSchema = it.credentialSchema ?: return VerificationPolicyResult.success()
            return validateSchema(it, URI.create(credentialSchema.id))
        }
    } catch (e: Exception) {
        VerificationPolicyResult.failure(e)
    }
}
