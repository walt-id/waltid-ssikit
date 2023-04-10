package id.walt.services.ecosystems.velocity.models

import kotlinx.serialization.Serializable

enum class CredentialCheckType {
    TRUSTED_ISSUER,
    UNREVOKED,
    UNEXPIRED,
    UNTAMPERED
}

enum class CredentialCheckValue {
    PASS,
    FAIL,
    VOUCHER_RESERVE_EXHAUSTED,
    SELF_SIGNED,
    NOT_APPLICABLE,
}

@Serializable
enum class ExchangeType {
    ISSUING
}

@Serializable
data class LocalizedString(
    val localized: Map<String, String>
)

data class VerificationResult(
    val checks: Map<CredentialCheckType, CredentialCheckValue>,
    val result: Boolean
)

data class CredentialCheckPolicyParam(
    val checkList: Map<CredentialCheckType, CredentialCheckValue>
)
