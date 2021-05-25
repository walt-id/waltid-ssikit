package org.letstrust.vclib.vcs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.letstrust.vclib.VCMetadata

@Serializable
data class Europass(
    @SerialName("@context")
    override val context: List<String>,
    var id: String?, // did:ebsi-eth:00000001/credentials/1872
    override val type: List<String>,
    var issuer: String?, // did:ebsi:00000001
    var issuanceDate: String?, // 2019-06-22T14:11:44Z
    val credentialSubject: CredentialSubject?,
    val learningAchievement: LearningAchievement?,
    val learningSpecificationReferences: LearningSpecificationReferences?,
    val targetDescription: TargetDescription?,
    val targetFrameworkName: TargetFrameworkName?,
    val credentialStatus: CredentialStatus?,
    val credentialSchema: CredentialSchema?,
    val evidence: Evidence?,
    val proof: Proof? = null
) : VC {

    companion object : VCMetadata {
        override val metadataContext = "https://essif.europa.eu/schemas/vc/2020/v1"
        override val metadataType = "Europass"
    }

    @Serializable
    data class CredentialSubject(
        @SerialName("@id")
        var id: String?, // did:essif:123121
        val currentFamilyName: String?, // Skywalker
        val currentGivenName: String?, // Lea
        val dateOfBirth: String?, // 2021-02-15
        val mailBox: MailBox?
    ) {
        @Serializable
        data class MailBox(
            @SerialName("@uri")
            val uri: String? // mailto:leaskywalker@gmail.com
        )
    }

    @Serializable
    data class LearningAchievement(
        @SerialName("@id")
        val id: String?, // urn:epass:learningAchievement:1
        var title: Title?,
        val specifiedBy: SpecifiedBy?
    ) {
        @Serializable
        data class Title(
            val text: Text?
        ) {
            @Serializable
            data class Text(
                @SerialName("@content-type")
                val contentType: String?, // text/plain
                @SerialName("@lang")
                val lang: String?, // en
                @SerialName("#text")
                var text: String? // Degree in Biology
            )
        }

        @Serializable
        data class SpecifiedBy(
            @SerialName("@idref")
            val idref: String? // urn:epass:qualification:1
        )
    }

    @Serializable
    data class LearningSpecificationReferences(
        val qualification: Qualification?
    ) {
        @Serializable
        data class Qualification(
            @SerialName("@id")
            val id: String?, // urn:epass:qualification:1
            val title: Title?,
            val awardingOpportunities: String?, // null
            val eqfLevel: EqfLevel?
        ) {
            @Serializable
            data class Title(
                val text: Text?
            ) {
                @Serializable
                data class Text(
                    @SerialName("@content-type")
                    val contentType: String?, // text/plain
                    @SerialName("@lang")
                    val lang: String?, // en
                    @SerialName("#text")
                    val text: String? // Degree in Biology
                )
            }

            @Serializable
            data class EqfLevel(
                @SerialName("@targetFrameworkUrl")
                val targetFrameworkUrl: String?, // http://data.europa.eu/snb/eqf/25831c2
                @SerialName("@targetNotation")
                val targetNotation: String?, // eqf
                @SerialName("@uri")
                val uri: String?, // http://data.europa.eu/snb/eqf/7
                val targetName: TargetName?
            ) {
                @Serializable
                data class TargetName(
                    val text: Text?
                ) {
                    @Serializable
                    data class Text(
                        @SerialName("@content-type")
                        val contentType: String?, // text/plain
                        @SerialName("@lang")
                        val lang: String?, // en
                        @SerialName("#text")
                        val text: String? // Level 7
                    )
                }
            }
        }
    }

    @Serializable
    data class TargetDescription(
        val text: Text?
    ) {
        @Serializable
        data class Text(
            @SerialName("@content-type")
            val contentType: String?, // text/plain
            @SerialName("@lang")
            val lang: String?, // en
            @SerialName("#text")
            val text: String? // Highly specialised knowledge, some of which is at the forefront of knowledge in a field of work or study, as the basis for original thinking and/or research <html:br></html:br>critical awareness of knowledge issues in a field and at the interface between different fields </html:div> <html:div xmlns:html="http://www.w3.org/1999/xhtml" class="Skill">specialised problem-solving skills required in research and/or innovation in order to develop new knowledge and procedures and to integrate knowledge from different fields</html:div> <html:div xmlns:html="http://www.w3.org/1999/xhtml" class="Competence">manage and transform work or study contexts that are complex, unpredictable and require new strategic approaches <html:br></html:br>take responsibility for contributing to professional knowledge and practice and/or for reviewing the strategic performance of teams
        )
    }

    @Serializable
    data class TargetFrameworkName(
        val text: Text?
    ) {
        @Serializable
        data class Text(
            @SerialName("@content-type")
            val contentType: String?, // text/plain
            @SerialName("@lang")
            val lang: String?, // en
            @SerialName("#text")
            val text: String? // European Qualifications Framework for lifelong learning - (2008/C 111/01)
        )
    }

    @Serializable
    data class CredentialStatus(
        val id: String?, // https://essif.europa.eu/status/43
        val type: String? // CredentialsStatusList2020
    )

    @Serializable
    data class CredentialSchema(
        val id: String?, // https://essif.europa.eu/tsr-123/verifiableattestation.json
        val type: String? // JsonSchemaValidator2018
    )

    @Serializable
    data class Evidence(
        val id: String?, // https://essif.europa.eu/evidence/f2aeec97-fc0d-42bf-8ca7-0548192d4231
        val type: List<String?>?,
        val verifier: String?, // https:// essif.europa.eu /issuers/48
        val evidenceDocument: List<String?>?
    )

//    @Serializable
//    data class Proof(
//        val type: String?, // EcdsaSecp256k1Signature2019
//        val created: String?, // 2019-06-22T14:11:44Z
//        val creator: String? = null,
//        val proofPurpose: String? = null, // assertionMethod
//        val verificationMethod: VerificationMethod? = null,
//        val jws: String?, // eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..TCYt5X
//        val proofValue: String? = null // BD21J4fdlnBvBA+y6D...fnC8Y=
//    ) {
//        @Serializable
//        data class VerificationMethod(
//            val type: String?, // EidasCertificate2019
//            @SerialName("CertSerial")
//            val certSerial: String? // 1088321447
//        )
//    }
}
