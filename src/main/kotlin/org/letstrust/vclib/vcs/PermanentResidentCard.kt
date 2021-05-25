package org.letstrust.vclib.vcs


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.letstrust.vclib.VCMetadata

@Serializable
data class PermanentResidentCard(
    @SerialName("@context")
    override val context: List<String>,
    override val type: List<String>,
    val credentialSubject: CredentialSubject?,
    var issuer: String?, // did:sov:staging:PiEVD2uU2qKEQ5oxx1BJ6A
    var issuanceDate: String?, // 2020-04-22T10:37:22Z
    var identifier: String?, // 83627465
    var name: String?, // Permanent Resident Card
    var description: String?, // Government of Example Permanent Resident Card.
    val proof: Proof? = null
) : VC {
    @Serializable
    data class CredentialSubject(
        var id: String?, // did:key:z6Mkte9e5E2GRozAgYyhktX7eTt9woCR4yJLnaqC88FQCSyY
        val type: List<String?>?,
        var givenName: String?, // JOHN
        var familyName: String?, // SMITH
        var gender: String?, // Male
        var image: String?, // data:image/png;base64,iVBORw0KGgo...kJggg==
        var residentSince: String?, // 2015-01-01
        var lprCategory: String?, // C09
        var lprNumber: String?, // 000-000-204
        var commuterClassification: String?, // C1
        var birthCountry: String?, // Bahamas
        var birthDate: String? // 1958-08-17
    )

    companion object : VCMetadata {
        override val metadataContext = "https://w3id.org/citizenship/v1"
        override val metadataType = "PermanentResidentCard"
    }
}

fun PermanentResidentCard.encode() = Json.encodeToString(this)
fun PermanentResidentCard.encodePretty() = Json { prettyPrint = true }.encodeToString(this)
