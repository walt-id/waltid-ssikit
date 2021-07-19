package org.letstrust.services.vc

import id.walt.servicematrix.BaseService
import id.walt.servicematrix.ServiceProvider
import id.walt.servicematrix.ServiceRegistry
import info.weboftrust.ldsignatures.LdProof
import kotlinx.serialization.Serializable
import org.letstrust.model.VerifiableCredential

enum class VerificationType {
    VERIFIABLE_CREDENTIAL,
    VERIFIABLE_PRESENTATION
}

@Serializable
data class VerificationResult(val verified: Boolean, val verificationType: VerificationType)

abstract class VCService : BaseService() {
    override val implementation get() = ServiceRegistry.getService<VCService>()

    open fun sign(
        issuerDid: String,
        jsonCred: String,
        domain: String? = null,
        nonce: String? = null,
        verificationMethod: String? = null,
        proofPurpose: String? = null
    ): String = implementation.sign(issuerDid, jsonCred, domain, nonce, verificationMethod)

    open fun verify(vcOrVp: String): VerificationResult = implementation.verify(vcOrVp)
    open fun verifyVc(issuerDid: String, vc: String): Boolean = implementation.verifyVc(issuerDid, vc)
    open fun verifyVc(vc: String): Boolean = implementation.verifyVc(vc)
    open fun verifyVp(vp: String): Boolean = implementation.verifyVp(vp)

    open fun present(vc: String, domain: String?, challenge: String?): String =
        implementation.present(vc, domain, challenge)

    open fun listVCs(): List<String> = implementation.listVCs()
    open fun listTemplates(): List<String> = implementation.listTemplates()

    open fun defaultVcTemplate(): VerifiableCredential = implementation.defaultVcTemplate()

    open fun loadTemplate(name: String): String = implementation.loadTemplate(name)

    open fun addProof(credMap: Map<String, String>, ldProof: LdProof): String =
        implementation.addProof(credMap, ldProof)

    companion object : ServiceProvider {
        override fun getService() = object : VCService() {}
    }
}
