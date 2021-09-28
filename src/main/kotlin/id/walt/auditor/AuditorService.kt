package id.walt.auditor

import com.beust.klaxon.Klaxon
import id.walt.services.WaltIdServices.log
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import id.walt.services.vc.VcUtils
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.VerifiableDiploma
import id.walt.vclib.vclist.VerifiablePresentation
import mu.KotlinLogging

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

val log = KotlinLogging.logger {}

interface VerificationPolicy {
    val id: String
        get() = this.javaClass.simpleName
    val description: String
    fun verify(vc: VerifiableCredential): Boolean
}

class SignaturePolicy : VerificationPolicy {
    private val jsonLdCredentialService = JsonLdCredentialService.getService()
    private val jwtCredentialService = JwtCredentialService.getService()
    override val description: String = "Verify by signature"

    override fun verify(vc: VerifiableCredential): Boolean {
        return DidService.importKey(VcUtils.getIssuer(vc)) && when (vc.jwt) {
            null -> jsonLdCredentialService.verify(vc.json!!).verified
            else -> jwtCredentialService.verify(vc.jwt!!).verified
        }
    }
}

class JsonSchemaPolicy : VerificationPolicy { // Schema already validated by json-ld?
    override val description: String = "Verify by JSON schema"
    override fun verify(vc: VerifiableCredential) = true // TODO validate policy
}

class TrustedIssuerDidPolicy : VerificationPolicy {
    override val description: String = "Verify by trusted issuer did"
    override fun verify(vc: VerifiableCredential): Boolean {

        //TODO complete PoC implementation
        return when (vc) {
            is VerifiablePresentation -> true
            else -> DidService.loadOrResolveAnyDid(VcUtils.getIssuer(vc)) != null
        }
    }
}

class TrustedSubjectDidPolicy : VerificationPolicy {
    override val description: String = "Verify by trusted subject did"
    override fun verify(vc: VerifiableCredential): Boolean {

        //TODO complete PoC implementation
        return when (vc) {
            is VerifiablePresentation -> true
            else -> DidService.loadOrResolveAnyDid(VcUtils.getHolder(vc)) != null
        }
    }
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
        register(sigPol)
        register(TrustedIssuerDidPolicy())
        register(TrustedSubjectDidPolicy())
        register(JsonSchemaPolicy())
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

    override fun verify(vcStr: String, policies: List<VerificationPolicy>): VerificationResult {
        val vc = vcStr.toCredential()
        val policyResults = policies.associateBy(keySelector = VerificationPolicy::id) { policy ->
            policy.verify(vc) &&
                    when (vc) {
                        is VerifiablePresentation -> vc.verifiableCredential.all { cred ->
                            policy.verify(cred)
                        }
                        else -> true
                    }
        }

        return VerificationResult(allAccepted(policyResults), policyResults)
    }
}
