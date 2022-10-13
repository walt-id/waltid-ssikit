package id.walt.model.oidc

data class CredentialIssuerDisplay(
    val name: String, val locale: String? = null
)

data class CredentialIssuer(
    val display: List<CredentialIssuerDisplay>
)
