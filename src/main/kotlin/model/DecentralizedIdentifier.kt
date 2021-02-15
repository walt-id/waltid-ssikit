package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// scheme did:, a method identifier, and a unique, method-specific identifier
// specified by the DID method. DIDs are resolvable to DID documents. A DID URL
// extends the syntax of a basic DID to incorporate other standard URI components
// such as path, query, and fragment in order to locate a particular resource
data class DidUrl(
    val method: String,
    val identifier: String,
    val fragment: String? = null,
    // TODO: query-params are not supported yet
    val query: List<String>? = null
)

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

