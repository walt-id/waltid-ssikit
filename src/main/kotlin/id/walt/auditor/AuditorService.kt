package id.walt.auditor

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
    fun id(): String
    fun verify(vp: VerifiablePresentation): Boolean
}

class SignaturePolicy() : VerificationPolicy {
    override fun id(): String = "SIGNATURE"

    override fun verify(vp: VerifiablePresentation): Boolean {
        // TODO validate policy
        return true
    }
}

class JsonSchemaPolicy() : VerificationPolicy {
    override fun id(): String = "JSON_SCHEMA"

    override fun verify(vp: VerifiablePresentation): Boolean {
        // TODO validate policy
        return true
    }
}

class TrustedIssuerDidPolicy() : VerificationPolicy {
    override fun id(): String = "TRUSTED_ISSUER_DID"

    override fun verify(vp: VerifiablePresentation): Boolean {
        // TODO validate policy
        return true
    }
}
class TrustedSubjectDidPolicy() : VerificationPolicy {
    override fun id(): String = "TRUSTED_SUBJECT_DID"

    override fun verify(vp: VerifiablePresentation): Boolean {
        // TODO validate policy
        return true
    }
}

object PolicyRegistry {
    val policies = HashMap<String, VerificationPolicy>()
    fun register(policy: VerificationPolicy) = policies.put(policy.id(), policy)
    fun getPolicy(id: String) = policies.get(id)!!
}

data class VerificationResult(
    val overallStatus: Boolean = false,
    val policyResults: Map<String, Boolean>
)

interface IAuditor {

    fun verify(vp: String, policies: List<String>): VerificationResult

//    fun verifyVc(vc: String, config: AuditorConfig): VerificationStatus {
//        return VerificationStatus(true)
//    }
//
//    fun verifyVp(vp: String, config: AuditorConfig): VerificationStatus {
//        return VerificationStatus(true)
//    }

}

object AuditorService : IAuditor {

    override fun verify(vp: String, policies: List<String>): VerificationResult {
        val vpObj = vp as VerifiablePresentation         // TODO load VerifiablePresentation
        val policyResults = HashMap<String, Boolean>()
        for(id in policies) {
            policyResults[id] = PolicyRegistry.getPolicy(id).verify(vpObj)
        }
        return VerificationResult(policyResults.values.all { it }, policyResults)
    }
}

fun main() {
    PolicyRegistry.register(SignaturePolicy())
    PolicyRegistry.register(TrustedIssuerDidPolicy())
    PolicyRegistry.register(TrustedSubjectDidPolicy())
    PolicyRegistry.register(JsonSchemaPolicy())

    val res = AuditorService.verify("{}", listOf("signature", "revocation"))
    println(res)
}
