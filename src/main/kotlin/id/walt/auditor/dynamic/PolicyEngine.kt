package id.walt.auditor.dynamic

import id.walt.auditor.VerificationPolicyResult

interface PolicyEngine {

    fun validate(input: PolicyEngineInput, policy: String, query: String): VerificationPolicyResult
    val type: PolicyEngineType

    companion object {
        fun get(type: PolicyEngineType): PolicyEngine {
            return when (type) {
                PolicyEngineType.OPA -> OPAPolicyEngine
            }
        }
    }
}
