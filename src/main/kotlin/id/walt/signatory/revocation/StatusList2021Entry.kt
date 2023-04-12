package id.walt.signatory.revocation

import kotlinx.serialization.Serializable

@Serializable
data class StatusList2021Entry(
    val id: String,
    val type: String = "StatusList2021Entry",
    val statusPurpose: String, //e.g. revocation or suspension
    val statusListIndex: String, //e.g. 94567
    val statusListCredential: String, //e.g. https://example.com/credentials/status/3
)
