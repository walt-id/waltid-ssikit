package id.walt.auditor.dynamic

import id.walt.auditor.VerificationPolicyResult

interface PolicyEngine {

    fun validate(input: Map<String, Any?>, data: Map<String, Any?>, policy: String, query: String): VerificationPolicyResult
    val type: PolicyEngineType

    companion object {
        fun get(type: PolicyEngineType): PolicyEngine {
            return when (type) {
                PolicyEngineType.OPA -> OPAPolicyEngine
            }
        }
    }
}
