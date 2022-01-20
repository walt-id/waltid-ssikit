package id.walt.model

class DidWeb(
    context: List<String>,
    id: String,
    verificationMethod: List<VerificationMethod>? = null,
    authentication: List<String>? = null,
    assertionMethod: List<String>? = null,
    capabilityDelegation: List<String>? = null,
    capabilityInvocation: List<String>? = null,
    keyAgreement: List<String>? = null,
    serviceEndpoint: List<VerificationMethod>? = null
) : Did(
    context,
    id,
    verificationMethod,
    authentication,
    assertionMethod,
    capabilityDelegation,
    capabilityInvocation,
    keyAgreement,
    serviceEndpoint
) {
    constructor(context: String,
                id: String,
                verificationMethod: List<VerificationMethod>? = null,
                authentication: List<String>? = null,
                assertionMethod: List<String>? = null,
                capabilityDelegation: List<String>? = null,
                capabilityInvocation: List<String>? = null,
                keyAgreement: List<String>? = null,
                serviceEndpoint: List<VerificationMethod>? = null) : this(listOf(context), id, verificationMethod, authentication, assertionMethod, capabilityDelegation, capabilityInvocation, keyAgreement, serviceEndpoint)
}

/*
@Serializable
data class DidWeb(
    @SerialName("@context")
    val context: String?,
    val id: String?,
    val verificationMethod: List<VerificationMethod>? = null,
    val keyAgreement: List<KeyAgreement?>? = null,
    val authentication: List<String?>? = null,
    val assertionMethod: List<String?>? = null,
    val capabilityDelegation: List<String?>? = null,
    val capabilityInvocation: List<String?>? = null,
    val serviceEndpoint: List<id.walt.model.VerificationMethod>? = null
) {
    @Serializable
    data class KeyAgreement(
        val id: String?, // did:web:did.actor:alice#zC8GybikEfyNaausDA4mkT4egP7SNLx2T1d1kujLQbcP6h
        val type: String?, // X25519KeyAgreementKey2019
        val controller: String?, // did:web:did.actor:alice
        val publicKeyBase58: String? // CaSHXEvLKS6SfN9aBfkVGBpp15jSnaHazqHgLHp8KZ3Y
    )
}

fun DidWeb.encode() = Klaxon().toJsonString(this)
fun DidWeb.encodePretty() = Klaxon().toJsonString(this)*/
