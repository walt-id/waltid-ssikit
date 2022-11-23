package id.walt.model

import id.walt.credentials.w3c.VerifiableCredential
import kotlinx.serialization.Serializable

@Serializable
data class VerifiableCredentialModel(
    val type: List<String>,
    val id: String,
    val issuer: String,
    val subject: String,
    val issuedOn: String
) {
    constructor(verifiableCredential: VerifiableCredential) : this(
        verifiableCredential.type,
        verifiableCredential.id.toString(),
        verifiableCredential.issuerId.toString(),
        verifiableCredential.subjectId.toString(),
        verifiableCredential.issued.toString()
    )
}
