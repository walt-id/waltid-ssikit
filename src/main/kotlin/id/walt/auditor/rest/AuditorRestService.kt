package id.walt.auditor.rest

import id.walt.auditor.Auditor
import id.walt.auditor.PolicyRegistry
import id.walt.auditor.VerificationResult

object AuditorRestService {

    fun verifyVP(reqPolicyList: List<String>, body: String): VerificationResult? {
        val policyList = reqPolicyList.flatMap {
            it.split(",")
                .map { policyName -> policyName.trim() }
        }

        val policies = policyList.ifEmpty { listOf(PolicyRegistry.defaultPolicyId) }

        return when {
            policies.any { !PolicyRegistry.contains(it) } -> null
            else -> Auditor.verify(body, policies.map { PolicyRegistry.getPolicy(it) })
        }
    }

}
