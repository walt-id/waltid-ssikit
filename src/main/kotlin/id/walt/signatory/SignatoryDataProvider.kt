package id.walt.signatory

import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.VerifiableDiploma
import id.walt.vclib.vclist.VerifiableId
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

interface SignatoryDataProvider {
    fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential
}

val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

class VerifiableIdDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableId {
        val vc = template as VerifiableId
        vc.setDefaultData()
        return when (proofConfig.proofType) {
            ProofType.LD_PROOF -> populateForLDProof(vc, proofConfig)
            ProofType.JWT -> populateForJWTProof(vc, proofConfig)
        }
    }

    private fun populateForLDProof(vc: VerifiableId, proofConfig: ProofConfig): VerifiableId {
        vc.id = proofConfig.id ?: "identity#verifiableID#${UUID.randomUUID()}"
        vc.issuer = proofConfig.issuerDid
        if (proofConfig.issueDate != null) vc.issuanceDate = dateFormat.format(proofConfig.issueDate)
        if (proofConfig.expirationDate != null) vc.expirationDate = dateFormat.format(proofConfig.expirationDate)
        vc.validFrom = vc.issuanceDate
        vc.credentialSubject!!.id = proofConfig.subjectDid
        return vc
    }

    private fun populateForJWTProof(vc: VerifiableId, proofConfig: ProofConfig): VerifiableId {
        if (proofConfig.issueDate != null) vc.validFrom = dateFormat.format(proofConfig.issueDate)
        return vc
    }
}

class VerifiableDiplomaDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableDiploma {
        val vc = template as VerifiableDiploma
        vc.setDefaultData()
        return when (proofConfig.proofType) {
            ProofType.LD_PROOF -> populateForLDProof(vc, proofConfig)
            ProofType.JWT -> populateForJWTProof(vc, proofConfig)
        }
    }

    private fun populateForLDProof(vc: VerifiableDiploma, proofConfig: ProofConfig): VerifiableDiploma {
        vc.id = proofConfig.id ?: "education#higherEducation#${UUID.randomUUID()}"
        vc.issuer = proofConfig.issuerDid
        if (proofConfig.issueDate != null) vc.issuanceDate = dateFormat.format(proofConfig.issueDate)
        if (proofConfig.expirationDate != null) vc.expirationDate = dateFormat.format(proofConfig.expirationDate)
        vc.validFrom = vc.issuanceDate
        vc.credentialSubject!!.id = proofConfig.subjectDid
        return vc
    }

    private fun populateForJWTProof(vc: VerifiableDiploma, proofConfig: ProofConfig): VerifiableDiploma {
        if (proofConfig.issueDate != null) vc.validFrom = dateFormat.format(proofConfig.issueDate)
        return vc
    }
}

class NoSuchDataProviderException(credentialType: KClass<out VerifiableCredential>) :
    Exception("No data provider is registered for ${credentialType.simpleName}")

object DataProviderRegistry {
    val providers = HashMap<KClass<out VerifiableCredential>, SignatoryDataProvider>()

    // TODO register via unique stringId
    fun register(credentialType: KClass<out VerifiableCredential>, provider: SignatoryDataProvider) =
        providers.put(credentialType, provider)

    fun getProvider(credentialType: KClass<out VerifiableCredential>) =
        providers[credentialType] ?: throw NoSuchDataProviderException(credentialType)

    init {
        // Init default providers
        register(VerifiableDiploma::class, VerifiableDiplomaDataProvider())
        register(VerifiableId::class, VerifiableIdDataProvider())
    }
}

fun VerifiableId.setDefaultData(): VerifiableId {
    credentialSubject = VerifiableId.CredentialSubject(
        familyName = "DOE",
        firstName = "Jane",
        dateOfBirth = "1993-04-08T00:00:00Z",
        personalIdentifier = "0904008084H",
        nameAndFamilyNameAtBirth = "Jane DOE",
        placeOfBirth = "LILLE, FRANCE",
        currentAddress = "1 Boulevard de la Liberté, 59800 Lille",
        gender = "FEMALE"
    )
    //  EBSI does not support credentialStatus yet
    //  credentialStatus = CredentialStatus(
    //      id = "https://essif.europa.eu/status/identity#verifiableID#51e42fda-cb0a-4333-b6a6-35cb147e1a88",
    //      type = "CredentialsStatusList2020"
    //  )
    evidence = VerifiableId.Evidence(
        id = "https://blockchain.univ-lille.fr/identity#V_ID_evidence",
        type = listOf("DocumentVerification"),
        verifier = "did:ebsi:2LGKvDMrNUPR6FhSNrXzQQ1h295zr4HwoX9UqvwAsenSKHe9",
        evidenceDocument = listOf("Passport"),
        subjectPresence = "Physical",
        documentPresence = listOf("Physical")
    )
    return this
}

fun VerifiableDiploma.setDefaultData(): VerifiableDiploma {
    credentialSubject = VerifiableDiploma.CredentialSubject(
        identifier = "0904008084H",
        givenNames = "Jane",
        familyName = "DOE",
        dateOfBirth = "1993-04-08T00:00:00Z",
        gradingScheme = VerifiableDiploma.CredentialSubject.GradingScheme(
            id = "https://blockchain.univ-lille.fr/ontology#GradingScheme",
            title = "Lower Second-Class Honours"
        ),
        learningAchievement = VerifiableDiploma.CredentialSubject.LearningAchievement(
            id = "https://blockchain.univ-lille.fr/ontology#LearningAchievment",
            title = "MASTERS LAW, ECONOMICS AND MANAGEMENT",
            description = "MARKETING AND SALES",
            additionalNote = listOf(
                "DISTRIBUTION MANAGEMENT"
            )
        ),
        awardingOpportunity = VerifiableDiploma.CredentialSubject.AwardingOpportunity(
            id = "https://blockchain.univ-lille.fr/ontology#AwardingOpportunity",
            identifier = "https://certificate-demo.bcdiploma.com/check/87ED2F2270E6C41456E94B86B9D9115B4E35BCCAD200A49B846592C14F79C86BV1Fnbllta0NZTnJkR3lDWlRmTDlSRUJEVFZISmNmYzJhUU5sZUJ5Z2FJSHpWbmZZ",
            awardingBody = VerifiableDiploma.CredentialSubject.AwardingOpportunity.AwardingBody(
                id = "did:ebsi:2LGKvDMrNUPR6FhSNrXzQQ1h295zr4HwoX9UqvwAsenSKHe9",
                eidasLegalIdentifier = "Unknown",
                registration = "0597065J",
                preferredName = "Université de Lille",
                homepage = "https://www.univ-lille.fr/"
            ),
            location = "FRANCE",
            startedAtTime = "2015-11-03T00:00:00Z",
            endedAtTime = "2020-11-03T00:00:00Z"
        ),
        learningSpecification = VerifiableDiploma.CredentialSubject.LearningSpecification(
            id = "https://blockchain.univ-lille.fr/ontology#LearningSpecification",
            iscedfCode = listOf(
                "7"
            ),
            ectsCreditPoints = 120,
            eqfLevel = 7,
            nqfLevel = listOf(
                "7"
            )
        )
    )
    //  EBSI does not support credentialStatus yet
    //  credentialStatus = CredentialStatus(
    //      id = "https://essif.europa.eu/status/education#higherEducation#51e42fda-cb0a-4333-b6a6-35cb147e1a88",
    //      type = "CredentialsStatusList2020"
    //  )
    return this
}
