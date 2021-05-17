package org.letstrust.model

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.letstrust.vclib.vcs.Europass
import org.letstrust.vclib.vcs.VC

@Serializable
data class VerifiablePresentation(
    @SerialName("@context")
    val context: List<String>,
    val id: String? = null,
    val type: List<String>,
    val verifiableCredential: List<VerifiableCredential>?,
    val vc: List<Europass>?,
    val proof: Proof? = null
)
