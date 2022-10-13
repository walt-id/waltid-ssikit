package id.walt.model.oidc

import com.beust.klaxon.Json

data class CredentialLogo(
    @Json(serializeNull = false) val url: String? = null,
    @Json(serializeNull = false) val alt_text: String? = null
)

data class CredentialDisplay(
    val name: String,
    @Json(serializeNull = false) val locale: String? = null,
    @Json(serializeNull = false) val logo: CredentialLogo? = null,
    @Json(serializeNull = false) val description: String? = null,
    @Json(serializeNull = false) val background_color: String? = null,
    @Json(serializeNull = false) val text_color: String? = null
)

data class ClaimDisplay(
    val name: String,
    @Json(serializeNull = false) val locale: String? = null
)

data class CredentialFormat(
    val types: List<String>,
    @Json(serializeNull = false) val cryptographic_binding_methods_supported: List<String>? = null,
    @Json(serializeNull = false) val cryptographic_suites_supported: List<String>? = null
)

data class CredentialClaimDefinition(
    @Json(serializeNull = false) val mandatory: Boolean? = null,
    @Json(serializeNull = false) val namespace: String? = null,
    @Json(serializeNull = false) val value_type: String? = null,
    @Json(serializeNull = false) val display: List<ClaimDisplay>? = null
)

data class CredentialMetadata(
    val formats: Map<String, CredentialFormat>,
    @Json(serializeNull = false) val display: List<CredentialDisplay>? = null,
    @Json(serializeNull = false) val claims: Map<String, CredentialClaimDefinition>? = null
)
