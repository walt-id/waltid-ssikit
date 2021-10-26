package deltadao

import id.walt.model.DidMethod
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.SignatoryDataProvider
import id.walt.vclib.model.VerifiableCredential
import java.util.*

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
) {}

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
