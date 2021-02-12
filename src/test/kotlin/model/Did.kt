package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EbsiDid(
    @SerialName("@context")  val context: String,
    val id: String,
    val authentication: List<Key>
)

@Serializable
data class Key(
    val id: String,
    val type: String,
    val controller: String,
    val publicKeyBase58: String
)
