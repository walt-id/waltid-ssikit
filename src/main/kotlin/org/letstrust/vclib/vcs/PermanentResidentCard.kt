package org.letstrust.vclib.vcs


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.letstrust.vclib.VCMetadata

@Serializable
data class PermanentResidentCard(
    @SerialName("@context")
    override val context: List<String>,
    override val type: List<String>,
    val credentialSubject: CredentialSubject?,
    val issuer: String?, // did:sov:staging:PiEVD2uU2qKEQ5oxx1BJ6A
    val issuanceDate: String?, // 2020-04-22T10:37:22Z
    val identifier: String?, // 83627465
    val name: String?, // Permanent Resident Card
    val description: String? // Government of Example Permanent Resident Card.
) : VC {
    @Serializable
    data class CredentialSubject(
        val id: String?, // did:key:z6Mkte9e5E2GRozAgYyhktX7eTt9woCR4yJLnaqC88FQCSyY
        val type: List<String?>?,
        val givenName: String?, // JOHN
        val familyName: String?, // SMITH
        val gender: String?, // Male
        val image: String?, // data:image/png;base64,iVBORw0KGgo...kJggg==
        val residentSince: String?, // 2015-01-01
        val lprCategory: String?, // C09
        val lprNumber: String?, // 000-000-204
        val commuterClassification: String?, // C1
        val birthCountry: String?, // Bahamas
        val birthDate: String? // 1958-08-17
    )

    companion object : VCMetadata {
        override val metadataContext = "https://w3id.org/citizenship/v1"
        override val metadataType = "PermanentResidentCard"
    }
}
