package id.walt.model

import com.beust.klaxon.Klaxon
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DidWeb(
    @SerialName("@context")
    val context: String?, // https://w3id.org/did/v0.11
    val id: String?, // did:web:did.actor:alice
    val publicKey: List<PublicKey?>?,
    val keyAgreement: List<KeyAgreement?>? = null,
    val authentication: List<String?>?,
    val assertionMethod: List<String?>?,
    val capabilityDelegation: List<String?>?,
    val capabilityInvocation: List<String?>?,
) {
    @Serializable
    data class PublicKey(
        val id: String?, // did:web:did.actor:alice#z6MkrmNwty5ajKtFqc1U48oL2MMLjWjartwc5sf2AihZwXDN
        val type: String?, // Ed25519VerificationKey2018
        val controller: String?, // did:web:did.actor:alice
        val publicKeyBase58: String? // DK7uJiq9PnPnj7AmNZqVBFoLuwTjT1hFPrk6LSjZ2JRz
    )

    @Serializable
    data class KeyAgreement(
        val id: String?, // did:web:did.actor:alice#zC8GybikEfyNaausDA4mkT4egP7SNLx2T1d1kujLQbcP6h
        val type: String?, // X25519KeyAgreementKey2019
        val controller: String?, // did:web:did.actor:alice
        val publicKeyBase58: String? // CaSHXEvLKS6SfN9aBfkVGBpp15jSnaHazqHgLHp8KZ3Y
    )
}

fun DidWeb.encode() = Klaxon().toJsonString(this)
fun DidWeb.encodePretty() = Klaxon().toJsonString(this)
