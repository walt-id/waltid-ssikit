package id.walt.signatory

import id.walt.model.DidMethod
import id.walt.services.did.DidService
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.credentials.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KClass

interface SignatoryDataProvider {
    fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential
}

val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")


object DataProviderRegistry {
    val providers = HashMap<KClass<out VerifiableCredential>, SignatoryDataProvider>()

    fun register(credentialType: KClass<out VerifiableCredential>, provider: SignatoryDataProvider) =
        providers.put(credentialType, provider)

    fun getProvider(credentialType: KClass<out VerifiableCredential>) =
        providers[credentialType] ?: throw NoSuchDataProviderException(credentialType)

    init {
        // Init default providers
        register(VerifiableAttestation::class, VerifiableAttestationDataProvider())
        register(VerifiableAuthorization::class, VerifiableAuthorizationDataProvider())
        register(VerifiableDiploma::class, VerifiableDiplomaDataProvider())
        register(VerifiableId::class, VerifiableIdDataProvider())
        register(Europass::class, EuropassDataProvider())
        register(GaiaxCredential::class, DeltaDaoDataProvider())
        register(PermanentResidentCard::class, PermanentResidentCardDataProvider())
    }
}


class VerifiableAttestationDataProvider : SignatoryDataProvider {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableAttestation {
        val vc = template as VerifiableAttestation
        vc.id = proofConfig.credentialId
        vc.issuer = proofConfig.issuerDid
        vc.credentialSubject!!.id = proofConfig.subjectDid!!
        return vc
    }
}

class VerifiableAuthorizationDataProvider : SignatoryDataProvider {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableAuthorization {
        val vc = template as VerifiableAuthorization
        vc.id = proofConfig.credentialId
        vc.issuer = proofConfig.issuerDid
        vc.credentialSubject.id = proofConfig.subjectDid!!
        return vc
    }
}

class PermanentResidentCardDataProvider : SignatoryDataProvider {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): PermanentResidentCard {
        val vc = template as PermanentResidentCard
        vc.id = proofConfig.credentialId
        vc.issuer = proofConfig.issuerDid
        vc.credentialSubject!!.id = proofConfig.subjectDid!!
        return vc
    }
}

class EuropassDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): Europass {
        val vc = template as Europass
        vc.id = proofConfig.credentialId
        vc.issuer = proofConfig.issuerDid
        vc.credentialSubject!!.id = proofConfig.subjectDid

        return vc
    }
}

class VerifiableIdDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableId {
        val vc = template as VerifiableId

        vc.id = proofConfig.credentialId ?: "identity#verifiableID#${UUID.randomUUID()}"
        vc.issuer = proofConfig.issuerDid
        if (proofConfig.issueDate != null) vc.issuanceDate = dateFormat.format(proofConfig.issueDate)
        if (proofConfig.validDate != null) vc.validFrom = dateFormat.format(proofConfig.validDate)
        if (proofConfig.expirationDate != null) vc.expirationDate = dateFormat.format(proofConfig.expirationDate)
        vc.validFrom = vc.issuanceDate
        vc.credentialSubject!!.id = proofConfig.subjectDid
        vc.evidence!!.verifier = proofConfig.issuerDid

        return vc
    }
}

class VerifiableDiplomaDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableDiploma {
        val vc = template as VerifiableDiploma

        vc.id = proofConfig.credentialId ?: "education#higherEducation#${UUID.randomUUID()}"
        vc.issuer = proofConfig.issuerDid
        if (proofConfig.issueDate != null) vc.issuanceDate = dateFormat.format(proofConfig.issueDate)
        if (proofConfig.validDate != null) vc.validFrom = dateFormat.format(proofConfig.validDate)
        if (proofConfig.expirationDate != null) vc.expirationDate = dateFormat.format(proofConfig.expirationDate)
        vc.credentialSubject!!.id = proofConfig.subjectDid
        vc.credentialSubject!!.awardingOpportunity!!.awardingBody.id = proofConfig.issuerDid

        return vc
    }
}

class NoSuchDataProviderException(credentialType: KClass<out VerifiableCredential>) :
    Exception("No data provider is registered for ${credentialType.simpleName}")


class DeltaDaoDataProvider : SignatoryDataProvider {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
        if (template is GaiaxCredential) {

            // TODO: Load and replace data wherever required
            // val idData = DeltaDaoDatabase.get(proofConfig.dataProviderIdentifier!!) ?: throw Exception("No ID data found for the given data-povider identifier")

            template.apply {
                id = "identity#verifiableID#${UUID.randomUUID()}"
                issuer = proofConfig.issuerDid
                credentialSubject.apply {
                    if (proofConfig.subjectDid != null) id = proofConfig.subjectDid
                    legallyBindingName = "deltaDAO AG"
                    brandName = "deltaDAO"
                    legallyBindingAddress = GaiaxCredential.CustomCredentialSubject.LegallyBindingAddress(
                        streetAddress = "Geibelstr. 46B",
                        postalCode = "22303",
                        locality = "Hamburg",
                        countryName = "Germany"
                    )
                    webAddress = GaiaxCredential.CustomCredentialSubject.WebAddress(
                        url = "https://www.delta-dao.com/"
                    )
                    corporateEmailAddress = "contact@delta-dao.com"
                    individualContactLegal = "legal@delta-dao.com"
                    individualContactTechnical = "support@delta-dao.com"
                    legalForm = "Stock Company"
                    jurisdiction = "Germany"
                    commercialRegister = GaiaxCredential.CustomCredentialSubject.CommercialRegister(
                        organizationName = "Amtsgericht Hamburg (-Mitte)",
                        organizationUnit = "Registergericht",
                        streetAddress = "Caffamacherreihe 20",
                        postalCode = "20355",
                        locality = "Hamburg",
                        countryName = "Germany"
                    )
                    legalRegistrationNumber = "HRB 170364"
                    ethereumAddress = GaiaxCredential.CustomCredentialSubject.EthereumAddress(
                        id = "0x4C84a36fCDb7Bc750294A7f3B5ad5CA8F74C4A52"
                    )
                    trustState = "trusted"
                }
            }
            return template
        } else {
            throw IllegalArgumentException("Only VerifiableId is supported by this data provider")
        }
    }
}


class IdData(
    val did: String,
    val familyName: String,
    val firstName: String,
    val dateOfBirth: String,
    val personalIdentifier: String,
    val nameAndFamilyNameAtBirth: String,
    val placeOfBirth: String,
    val currentAddress: String,
    val gender: String
)

object DeltaDaoDatabase {
    var mockedIds: Map<String, IdData>

    init {
        // generate id data
        val did1 = DidService.create(DidMethod.key)
        val did2 = DidService.create(DidMethod.key)

        val personalIdentifier1 = "0x4C84a36fCDb7Bc750294A7f3B5ad5CA8F74C4A52"
        val personalIdentifier2 = "0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7"

        mockedIds = mapOf(
            Pair(
                personalIdentifier1, IdData(
                    did1,
                    "DOE",
                    "Jane",
                    "1993-04-08",
                    personalIdentifier1,
                    "Jane DOE",
                    "LILLE, FRANCE",
                    "1 Boulevard de la Liberté, 59800 Lille",
                    "FEMALE"
                )
            ),
            Pair(
                personalIdentifier2, IdData(
                    did2,
                    "JAMES",
                    "Chris",
                    "1994-02-18",
                    personalIdentifier2,
                    "Christ JAMES",
                    "VIENNA, AUSTRIA",
                    "Mariahilferstraße 100, 1070 Wien",
                    "MALE"
                )
            )
        )
    }

    fun get(identifier: String): IdData? {
        return mockedIds[identifier]
    }
}
