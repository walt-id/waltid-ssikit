package id.walt.model

import id.walt.vclib.model.VerifiableCredential
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
        verifiableCredential.issuer.toString(),
        verifiableCredential.subject.toString(),
        verifiableCredential.issued.toString()
    )
}
