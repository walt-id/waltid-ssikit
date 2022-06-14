package id.walt.auditor

import id.walt.vclib.NestedVCs
import id.walt.vclib.model.VerifiableCredential

data class PolicyRequest (
  val policy: String,
  val argument: Map<String, Any?>? = null
    )

data class VerificationRequest (
  val policies: List<PolicyRequest>,
  @NestedVCs val credentials: List<VerifiableCredential>
    ) {
}

data class VerificationResponse (
  val valid: Boolean,
  val results: List<VerificationResult>
    )
