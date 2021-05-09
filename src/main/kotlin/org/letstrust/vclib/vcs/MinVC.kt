package org.letstrust.vclib.vcs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MinVC(
    @SerialName("@context")
    override val context: List<String>,
    override val type: List<String>
) : VC
