package id.walt.auditor

import id.walt.services.vc.JsonLdCredentialService
import id.walt.vclib.Helpers.encode
import id.walt.vclib.VcLibManager
import id.walt.vclib.vclist.VerifiablePresentation

// the following validation policies can be applied
// - SIGNATURE
// - JSON_SCHEMA
// - TRUSTED_ISSUER_DID
// - TRUSTED_SUBJECT_DID
// - REVOCATION_STATUS
// - ISSUANCE_DATA_AFTER
// - EXPIRATION_DATE_BEFORE
// - REVOCATION_STATUS
// - SECURE_CRYPTO
// - HOLDER_BINDING (only for VPs)


interface VerificationPolicy {
    fun id(): String = this.javaClass.simpleName
    fun verify(vp: VerifiablePresentation): Boolean
}

class SignaturePolicy : VerificationPolicy {
    private val jsonLdCredentialService = JsonLdCredentialService.getService()


    override fun verify(vp: VerifiablePresentation): Boolean {
        return jsonLdCredentialService.verify(vp.encode()).verified
    }
}

class JsonSchemaPolicy : VerificationPolicy { // Schema already validated by json-ld?
    override fun verify(vp: VerifiablePresentation) = true // TODO validate policy
}

class TrustedIssuerDidPolicy : VerificationPolicy {
    override fun verify(vp: VerifiablePresentation) = true // TODO validate policy
}

class TrustedSubjectDidPolicy : VerificationPolicy {
    override fun verify(vp: VerifiablePresentation) = true // TODO validate policy
}

object PolicyRegistry {
    val policies = HashMap<String, VerificationPolicy>()

    fun register(policy: VerificationPolicy) = policies.put(policy.id(), policy)
    fun getPolicy(id: String) = policies[id]!!
}

data class VerificationResult(
    val overallStatus: Boolean = false,
    val policyResults: Map<VerificationPolicy, Boolean>
) {
    override fun toString() =
        "VerificationResult(overallStatus=$overallStatus, policyResults={${policyResults.entries.joinToString { it.key.javaClass.simpleName + "=" + it.value }}})"
}

interface IAuditor {

    fun verify(vpJson: String, policies: List<VerificationPolicy>): VerificationResult
    fun verifyByIds(vpJson: String, policies: List<String>): VerificationResult =
        verify(vpJson, policies.map { PolicyRegistry.getPolicy(it) })

//    fun verifyVc(vc: String, config: AuditorConfig) = VerificationStatus(true)
//    fun verifyVp(vp: String, config: AuditorConfig) = VerificationStatus(true)
}

object AuditorService : IAuditor {

    private fun allAccepted(policyResults: Map<VerificationPolicy, Boolean>) = policyResults.values.all { it }


    override fun verify(vpJson: String, policies: List<VerificationPolicy>): VerificationResult {
        val vp = VcLibManager.getVerifiableCredential(vpJson) as VerifiablePresentation

        val policyResults = policies.associateWith { it.verify(vp) }

        return VerificationResult(allAccepted(policyResults), policyResults)
    }

    init {
        PolicyRegistry.register(SignaturePolicy())
        PolicyRegistry.register(TrustedIssuerDidPolicy())
        PolicyRegistry.register(TrustedSubjectDidPolicy())
        PolicyRegistry.register(JsonSchemaPolicy())
    }
}
