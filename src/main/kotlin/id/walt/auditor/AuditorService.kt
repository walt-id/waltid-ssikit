package id.walt.auditor

import id.walt.services.vc.JsonLdCredentialService
import id.walt.vclib.Helpers.encode
import id.walt.vclib.VcLibManager
import id.walt.vclib.model.CredentialSchema
import id.walt.vclib.model.CredentialStatus
import id.walt.vclib.vclist.Europass
import id.walt.vclib.vclist.VerifiablePresentation

// the following validation policies can be applied
// - SIGNATURE
// - JSON_SCHEMA
// - TRUSTED_ISSUER_DID
// - TRUSTED_SUBJECT_DID
// - REVOCATION_STATUS
// - ISSUANCE_DATA_AFTER
// - EXPIRATION_DATE_BEFORE
// - REVOCATION_STATUS
// - SECURE_CRYPTO
// - HOLDER_BINDING (only for VPs)


interface VerificationPolicy {
    fun id(): String = this.javaClass.simpleName
    fun verify(vp: VerifiablePresentation): Boolean
}

class SignaturePolicy : VerificationPolicy {
    private val jsonLdCredentialService = JsonLdCredentialService.getService()

    override fun verify(vp: VerifiablePresentation) = jsonLdCredentialService.verifyVp(vp.encode())
}

class JsonSchemaPolicy : VerificationPolicy { // Schema already validated by json-ld?
    override fun verify(vp: VerifiablePresentation) = true // TODO validate policy
}

class TrustedIssuerDidPolicy : VerificationPolicy {
    override fun verify(vp: VerifiablePresentation) = true // TODO validate policy
}

class TrustedSubjectDidPolicy : VerificationPolicy {
    override fun verify(vp: VerifiablePresentation) = true // TODO validate policy
}

object PolicyRegistry {
    val policies = HashMap<String, VerificationPolicy>()
    fun register(policy: VerificationPolicy) = policies.put(policy.id(), policy)
    fun getPolicy(id: String) = policies[id]!!
}

data class VerificationResult(
    val overallStatus: Boolean = false,
    val policyResults: Map<String, Boolean>
)

interface IAuditor {

    fun verify(vpJson: String, policies: List<String>): VerificationResult

//    fun verifyVc(vc: String, config: AuditorConfig) = VerificationStatus(true)
//    fun verifyVp(vp: String, config: AuditorConfig) = VerificationStatus(true)
}

object AuditorService : IAuditor {

    private fun allAccepted(policyResults: Map<String, Boolean>) = policyResults.values.all { it }

    override fun verify(vpJson: String, policies: List<String>): VerificationResult {
        val vp = VcLibManager.getVerifiableCredential(vpJson) as VerifiablePresentation

        val policyResults = policies.associateWith { PolicyRegistry.getPolicy(it).verify(vp) }

        return VerificationResult(allAccepted(policyResults), policyResults)
    }
}

fun main() {
    PolicyRegistry.register(SignaturePolicy())
    PolicyRegistry.register(TrustedIssuerDidPolicy())
    PolicyRegistry.register(TrustedSubjectDidPolicy())
    PolicyRegistry.register(JsonSchemaPolicy())

    val res = AuditorService.verify(
        VerifiablePresentation(
            id = "id",
            verifiableCredential = listOf(
                Europass(
                    id = "education#higherEducation#51e42fda-cb0a-4333-b6a6-35cb147e1a88",
                    issuer = "did:ebsi:2LGKvDMrNUPR6FhSNrXzQQ1h295zr4HwoX9UqvwAsenSKHe9",
                    issuanceDate = "2020-11-03T00:00:00Z",
                    validFrom = "2020-11-03T00:00:00Z",
                    credentialSubject = Europass.CredentialSubject(
                        id = "did:ebsi:22AhtW7XMssv7es4YcQTdV2MCM3c8b1VsiBfi5weHsjcCY9o",
                        identifier = "0904008084H",
                        givenNames = "Jane",
                        familyName = "DOE",
                        dateOfBirth = "1993-04-08",
                        gradingScheme = Europass.CredentialSubject.GradingScheme(
                            id = "https://blockchain.univ-lille.fr/ontology#GradingScheme",
                            title = "Lower Second-Class Honours"
                        ),
                        learningAchievement = Europass.CredentialSubject.LearningAchievement(
                            id = "https://blockchain.univ-lille.fr/ontology#LearningAchievment",
                            title = "MASTERS LAW, ECONOMICS AND MANAGEMENT",
                            description = "MARKETING AND SALES",
                            additionalNote = listOf(
                                "DISTRIBUTION MANAGEMENT"
                            )
                        ),
                        awardingOpportunity = Europass.CredentialSubject.AwardingOpportunity(
                            id = "https://blockchain.univ-lille.fr/ontology#AwardingOpportunity",
                            identifier = "https://certificate-demo.bcdiploma.com/check/87ED2F2270E6C41456E94B86B9D9115B4E35BCCAD200A49B846592C14F79C86BV1Fnbllta0NZTnJkR3lDWlRmTDlSRUJEVFZISmNmYzJhUU5sZUJ5Z2FJSHpWbmZZ",
                            awardingBody = Europass.CredentialSubject.AwardingOpportunity.AwardingBody(
                                id = "did:ebsi:2LGKvDMrNUPR6FhSNrXzQQ1h295zr4HwoX9UqvwAsenSKHe9",
                                eidasLegalIdentifier = "Unknown",
                                registration = "0597065J",
                                preferredName = "Université de Lille",
                                homepage = "https://www.univ-lille.fr/"
                            ),
                            location = "FRANCE",
                            startedAtTime = "Unknown",
                            endedAtTime = "2020-11-03T00:00:00Z"
                        ),
                        learningSpecification = Europass.CredentialSubject.LearningSpecification(
                            id = "https://blockchain.univ-lille.fr/ontology#LearningSpecification",
                            iSCEDFCode = listOf(
                                "7"
                            ),
                            eCTSCreditPoints = 120,
                            eQFLevel = 7,
                            nQFLevel = listOf(
                                "7"
                            )
                        )
                    ),
                    credentialStatus = CredentialStatus(
                        id = "https://essif.europa.eu/status/education#higherEducation#51e42fda-cb0a-4333-b6a6-35cb147e1a88",
                        type = "CredentialsStatusList2020"
                    ),
                    credentialSchema = CredentialSchema(
                        id = "https://essif.europa.eu/trusted-schemas-registry/v1/schemas/to_be_obtained_after_registration_of_the_schema",
                        type = "JsonSchemaValidator2018"
                    )
                )
            )
        ).encode(), listOf("SignaturePolicy", "JsonSchemaPolicy")
    )

    println(res)
}
