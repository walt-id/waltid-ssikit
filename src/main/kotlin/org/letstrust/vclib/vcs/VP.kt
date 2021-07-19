package org.letstrust.vclib.vcs

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.letstrust.model.Proof
import org.letstrust.vclib.VCMetadata


@Serializable
abstract class VP(
    @SerialName("@context")
    override val context: List<String>,
    override val type: List<String>,
    val id: String? = null,
    val vc: List<@Polymorphic VC>?,
    val proof: Proof? = null
) : VC {

    override fun issuer(): String = proof!!.creator!!

    override fun holder(): String = proof!!.creator!!

    companion object : VCMetadata {
        override val metadataContext = ""
        override val metadataType = "VerifiablePresentation"
    }
}

fun VP.encode() = Json.encodeToString(this)
fun VP.encodePretty() = Json { prettyPrint = true }.encodeToString(this)
