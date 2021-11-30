package id.walt.signatory

import id.walt.model.DidMethod
import id.walt.services.did.DidService
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.credentials.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KClass

interface SignatoryServiceOffering {
    fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential
}

val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")


object ServiceOfferingRegistry {
    val providers = HashMap<KClass<out VerifiableCredential>, SignatoryServiceOffering>()

    fun register(credentialType: KClass<out VerifiableCredential>, provider: SignatoryServiceOffering) =
        providers.put(credentialType, provider)

    fun getProvider(credentialType: KClass<out VerifiableCredential>) =
        providers[credentialType] ?: throw NoSuchServiceOfferingException(credentialType)

    init {
        // Init default providers
        register(VerifiableAttestation::class, VerifiableAttestationServiceOffering())
        register(VerifiableAuthorization::class, VerifiableAuthorizationServiceOffering())
        register(VerifiableDiploma::class, VerifiableDiplomaServiceOffering())
        register(VerifiableId::class, VerifiableIdServiceOffering())
        register(Europass::class, EuropassServiceOffering())
        register(GaiaxCredential::class, DeltaDaoServiceOffering())
        register(PermanentResidentCard::class, PermanentResidentCardServiceOffering())
    }
}


class VerifiableAttestationServiceOffering : SignatoryServiceOffering {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableAttestation {
        val vc = template as VerifiableAttestation
        vc.id = proofConfig.credentialId
        vc.issuer = proofConfig.issuerDid
        vc.credentialSubject!!.id = proofConfig.subjectDid!!
        return vc
    }
}

class VerifiableAuthorizationServiceOffering : SignatoryServiceOffering {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableAuthorization {
        val vc = template as VerifiableAuthorization
        vc.id = proofConfig.credentialId
        vc.issuer = proofConfig.issuerDid
        vc.credentialSubject.id = proofConfig.subjectDid!!
        return vc
    }
}

class PermanentResidentCardServiceOffering : SignatoryServiceOffering {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): PermanentResidentCard {
        val vc = template as PermanentResidentCard
        vc.id = proofConfig.credentialId
        vc.issuer = proofConfig.issuerDid
        vc.credentialSubject!!.id = proofConfig.subjectDid!!
        return vc
    }
}

class EuropassServiceOffering : SignatoryServiceOffering {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): Europass {
        val vc = template as Europass
        vc.id = proofConfig.credentialId
        vc.issuer = proofConfig.issuerDid
        vc.credentialSubject!!.id = proofConfig.subjectDid

        return vc
    }
}

class VerifiableIdServiceOffering : SignatoryServiceOffering {

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

class VerifiableDiplomaServiceOffering : SignatoryServiceOffering {

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

class NoSuchServiceOfferingException(credentialType: KClass<out VerifiableCredential>) :
    Exception("No service offering is registered for ${credentialType.simpleName}")


class GaiaXserviceOffering : SignatoryServiceOffering {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
        if (template is GaiaxCredential) {

            // TODO: Load and replace data wherever required
            // val idData = DeltaDaoDatabase.get(proofConfig.serviceOfferingIdentifier!!) ?: throw Exception("No ID data found for the given service offering identifier")

            template.apply {
                id = "identity#verifiableID#${UUID.randomUUID()}"
                issuer = proofConfig.issuerDid
                credentialSubject.apply {
                    if (proofConfig.subjectDid != null) id = proofConfig.subjectDid
                    hasServiceTitle = "Gaia-X Hackathon Registration"
                    hasServiceDescription = "This service can be used to register for the Gaia-X Hackathon #2, which runs from 02-12-2021 to 03-12-2021."
                    providedBy = "Cloud and Heat"
                    hasKeywords = "Gaia-X, Hackathon, Registration"
                    brandName = "deltaDAO"
                    webAddress = GaiaxCredential.CustomCredentialSubject.WebAddress(
                        url = "https://mautic.cloudandheat.com/gaiaxhackathon2"
                    )
                    hasProvisionType = "undefined"
                    hasServiceModel = "undefined"
                    trustState = "trusted"
                }
            }
            return template
        } else {
            throw IllegalArgumentException("Only VerifiableId is supported by this service offering")
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
