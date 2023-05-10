package id.walt.services.vc

import id.walt.auditor.VerificationPolicyResult
import id.walt.credentials.w3c.PresentableCredential
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.signatory.ProofConfig
import info.weboftrust.ldsignatures.LdProof
import kotlinx.serialization.Serializable
import java.net.URI
import java.time.Instant

enum class VerificationType {
    VERIFIABLE_CREDENTIAL,
    VERIFIABLE_PRESENTATION
}

@Serializable
data class VerificationResult(val verified: Boolean, val verificationType: VerificationType)

abstract class JsonLdCredentialService : WaltIdService() {
    override val implementation get() = serviceImplementation<JsonLdCredentialService>()

    open fun sign(jsonCred: String, config: ProofConfig): String = implementation.sign(jsonCred, config)

    open fun verify(vcOrVp: String): VerificationResult = implementation.verify(vcOrVp)

    open fun present(
        vcs: List<PresentableCredential>,
        holderDid: String,
        domain: String?,
        challenge: String?,
        expirationDate: Instant?
    ): String =
        implementation.present(vcs, holderDid, domain, challenge, expirationDate)

    open fun listVCs(): List<String> = implementation.listVCs()

    open fun addProof(credMap: Map<String, String>, ldProof: LdProof): String = implementation.addProof(credMap, ldProof)

    open fun validateSchema(vc: VerifiableCredential, schemaURI: URI): VerificationPolicyResult = implementation.validateSchema(vc, schemaURI)
    open fun validateSchemaTsr(vc: String): VerificationPolicyResult = implementation.validateSchemaTsr(vc)

    companion object : ServiceProvider {
        override fun getService() = object : JsonLdCredentialService() {}
        override fun defaultImplementation() = WaltIdJsonLdCredentialService()
    }
}
