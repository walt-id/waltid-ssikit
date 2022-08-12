package id.walt.services.velocitynetwork.models

data class VerificationResult(
    val checks: Map<CredentialCheckType, CredentialCheckValue>,
    val result: Boolean
)
