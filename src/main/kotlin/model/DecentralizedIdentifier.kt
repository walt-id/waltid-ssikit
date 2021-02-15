package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DidEbsi(
    @SerialName("@context")
    val context: String,
    var id: String? = null,
    val authentication: List<Key>? = null
)

@Serializable
data class Key(
    val id: String,
    val type: String,
    val controller: String,
    val publicKeyBase58: String
)


@Serializable
data class DidKey(
    @SerialName("@context")
    val context: String,
    val id: String,
    val publicKey: List<Key>,
    val authentication: List<String>,
    val assertionMethod: List<String>,
    val capabilityDelegation: List<String>,
    val capabilityInvocation: List<String>,
    val keyAgreement: List<Key>,
)

