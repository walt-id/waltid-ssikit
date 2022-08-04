package id.walt.services.velocitynetwork.models.requests

enum class CredentialCheck {
    TRUSTED_ISSUER,
    UNREVOKED,
    UNEXPIRED,
    UNTAMPERED
}