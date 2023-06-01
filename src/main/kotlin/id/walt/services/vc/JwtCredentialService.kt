package id.walt.services.vc

import id.walt.auditor.VerificationPolicyResult
import id.walt.credentials.w3c.PresentableCredential
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.signatory.ProofConfig
import java.net.URI
import java.time.Instant

abstract class JwtCredentialService : WaltIdService() {
    override val implementation get() = serviceImplementation<JwtCredentialService>()

    open fun sign(jsonCred: String, config: ProofConfig): String = implementation.sign(jsonCred, config)

    open fun verify(vcOrVp: String): VerificationResult = implementation.verify(vcOrVp)
    open fun verifyVc(issuerDid: String, vc: String): Boolean = implementation.verifyVc(issuerDid, vc)
    open fun verifyVc(vc: String): Boolean = implementation.verifyVc(vc)
    open fun verifyVp(vp: String): Boolean = implementation.verifyVp(vp)

    open fun present(
        vcs: List<PresentableCredential>,
        holderDid: String,
        verifierDid: String? = null,
        challenge: String? = null,
        expirationDate: Instant? = null
    ): String = implementation.present(vcs, holderDid, verifierDid, challenge, expirationDate)

    open fun listVCs(): List<String> = implementation.listVCs()

    open fun validateSchema(vc: VerifiableCredential, schemaURI: URI): VerificationPolicyResult = implementation.validateSchema(vc, schemaURI)
    open fun validateSchemaTsr(vc: String): VerificationPolicyResult = implementation.validateSchemaTsr(vc)

    companion object : ServiceProvider {
        override fun getService() = object : JwtCredentialService() {}
        override fun defaultImplementation() = WaltIdJwtCredentialService()
    }
}
