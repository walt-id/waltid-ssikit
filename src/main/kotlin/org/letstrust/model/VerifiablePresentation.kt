package org.letstrust.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifiablePresentation(
    @SerialName("@context")
    val context: List<String>,
    val id: String? = null,
    val type: List<String>,
    val verifiableCredential: List<VerifiableCredential>,
    val proof: Proof? = null
)
