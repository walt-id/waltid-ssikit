package id.walt.auditor

import id.walt.services.jwt.JwtService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import id.walt.signatory.ProofType
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.VcLibManager
import id.walt.vclib.model.VerifiableCredential
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

class VerificationItem(
    val vc: VerifiableCredential,
    val vcRaw: String,
    val proofType: ProofType) {

    companion object {
        val JWT_PATTERN = "(^[A-Za-z0-9-_]*\\.[A-Za-z0-9-_]*\\.[A-Za-z0-9-_]*\$)"
        fun parse(vc: String): VerificationItem {
            if(Regex(JWT_PATTERN).matches(vc))
            {
                val claims = JwtService.getService().parseClaims(vc)
                if (claims?.contains("vc") == true) {
                    var parsedVC = claims["vc"].toString().toCredential();
                    // TODO: how to set standard jwt claims (why are they stripped by data providers on issuance? I think they shouldn't be.)
                    return VerificationItem(parsedVC, vc, ProofType.JWT)
                } else {
                    throw Exception("Invalid JWT token given")
                }
            } else
            {
                return VerificationItem(vc.toCredential(), vc, ProofType.LD_PROOF)
            }
        }
    }
}

interface VerificationPolicy {
    val id: String
        get() = this.javaClass.simpleName
    val description: String
    fun verify(item: VerificationItem): Boolean
}

class SignaturePolicy : VerificationPolicy {
    private val jsonLdCredentialService = JsonLdCredentialService.getService()
    private val jwtCredentialService = JwtCredentialService.getService()
    override val description: String = "Verify by signature"

    override fun verify(item: VerificationItem): Boolean {
        return when(item.proofType) {
            // TODO: support JWT Presentation
            ProofType.JWT -> jwtCredentialService.verifyVc(item.vcRaw)
            ProofType.LD_PROOF -> jsonLdCredentialService.verify(item.vcRaw).verified
        }
    }
}

class JsonSchemaPolicy : VerificationPolicy { // Schema already validated by json-ld?
    override val description: String = "Verify by JSON schema"
    override fun verify(item: VerificationItem) = true // TODO validate policy
}

class TrustedIssuerDidPolicy : VerificationPolicy {
    override val description: String = "Verify by trusted issuer did"
    override fun verify(item: VerificationItem) = true // TODO validate policy
}

class TrustedSubjectDidPolicy : VerificationPolicy {
    override val description: String = "Verify by trusted subject did"
    override fun verify(item: VerificationItem) = true // TODO validate policy
}

object PolicyRegistry {
    val policies = HashMap<String, VerificationPolicy>()
    val defaultPolicyId: String

    fun register(policy: VerificationPolicy) = policies.put(policy.id, policy)
    fun getPolicy(id: String) = policies[id]!!
    fun contains(id: String) = policies.containsKey(id)
    fun listPolicies() = policies.values

    init {
        val sigPol = SignaturePolicy()
        defaultPolicyId = sigPol.id
        PolicyRegistry.register(sigPol)
        PolicyRegistry.register(TrustedIssuerDidPolicy())
        PolicyRegistry.register(TrustedSubjectDidPolicy())
        PolicyRegistry.register(JsonSchemaPolicy())
    }
}

data class VerificationResult(
    val overallStatus: Boolean = false,
    val policyResults: Map<String, Boolean>
) {
    override fun toString() =
        "VerificationResult(overallStatus=$overallStatus, policyResults={${policyResults.entries.joinToString { it.key + "=" + it.value }}})"
}

interface IAuditor {

    fun verify(vc: String, policies: List<VerificationPolicy>): VerificationResult

//    fun verifyVc(vc: String, config: AuditorConfig) = VerificationStatus(true)
//    fun verifyVp(vp: String, config: AuditorConfig) = VerificationStatus(true)
}

object AuditorService : IAuditor {

    private fun allAccepted(policyResults: Map<String, Boolean>) = policyResults.values.all { it }

    override fun verify(vc: String, policies: List<VerificationPolicy>): VerificationResult {
        val item = VerificationItem.parse(vc)
        val policyResults = policies.associateBy(keySelector = VerificationPolicy::id, { it.verify(item) })

        return VerificationResult(allAccepted(policyResults), policyResults)
    }
}
