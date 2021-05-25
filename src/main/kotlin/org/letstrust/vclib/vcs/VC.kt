package org.letstrust.vclib.vcs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.letstrust.vclib.VcLibManager

interface VC {
    @SerialName("@context")
    val context: List<String>
    val type: List<String>

    companion object {
        fun decode(json: String): VC = VcLibManager.getVerifiableCredential(json)
    }
}

@Serializable
data class Proof(
    val type: String?, // EcdsaSecp256k1Signature2019
    val created: String?, // 2019-06-22T14:11:44Z
    val creator: String? = null,
    val domain: String? = null,
    val nonce: String? = null,
    val jws: String?, // eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..TCYt5X
)
