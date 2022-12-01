package id.walt.auditor

import id.walt.common.VCList
import id.walt.credentials.w3c.VerifiableCredential

data class PolicyRequest(
    val policy: String,
    val argument: Map<String, Any?>? = null
)

data class VerificationRequest(
    val policies: List<PolicyRequest>,
    @VCList val credentials: List<VerifiableCredential>
)

data class VerificationResponse(
    val valid: Boolean,
    val results: List<VerificationResult>
)
