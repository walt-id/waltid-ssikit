package org.letstrust.vclib.vcs

import kotlinx.serialization.SerialName

interface VC {
    @SerialName("@context")
    val context: List<String>
    val type: List<String>
}
