package deltadao

import com.beust.klaxon.Json
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.registry.VerifiableCredentialMetadata

data class GaiaxCredential(
    @Json(name = "@context")
    var context: List<String> = listOf("https://www.w3.org/2018/credentials/v1"),
    @Json(serializeNull = false) var credentialSubject: CustomCredentialSubject? = null,
    @Json(serializeNull = false) var issuer: String? = null,
    @Json(serializeNull = false) var proof: Proof? = null,
) : VerifiableCredential(type) {
    data class CustomCredentialSubject(
        val legallyBindingName: String, // deltaDAO AG
        val brandName: String, // deltaDAO
        val legallyBindingAddress: LegallyBindingAddress,
        val webAddress: WebAddress,
        val corporateEmailAddress: String, // contact@delta-dao.com
        val individualContactLegal: String, // legal@delta-dao.com
        val individualContactTechnical: String, // support@delta-dao.com
        val legalForm: String, // Stock Company
        val jurisdiction: String, // Germany
        val commercialRegister: CommercialRegister,
        val legalRegistrationNumber: String, // HRB 170364
        val ethereumAddress: EthereumAddress,
        val trustState: String // trusted
    ) {
        data class LegallyBindingAddress(
            val streetAddress: String, // Geibelstr. 46B
            val postalCode: String, // 22303
            val locality: String, // Hamburg
            val countryName: String // Germany
        )

        data class WebAddress(
            val url: String // https://www.delta-dao.com/
        )

        data class CommercialRegister(
            val organizationName: String, // Amtsgericht Hamburg (-Mitte)
            val organizationUnit: String, // Registergericht
            val streetAddress: String, // Caffamacherreihe 20
            val postalCode: String, // 20355
            val locality: String, // Hamburg
            val countryName: String // Germany
        )

        data class EthereumAddress(
            val id: String // 0x4C84a36fCDb7Bc750294A7f3B5ad5CA8F74C4A52
        )
    }

    companion object : VerifiableCredentialMetadata(
        type = listOf("VerifiableCredential", "GaiaxCredential"),
        template = {
            GaiaxCredential(
                credentialSubject = CustomCredentialSubject(
                    legallyBindingName = "deltaDAO AG",
                    brandName = "deltaDAO",
                    legallyBindingAddress = CustomCredentialSubject.LegallyBindingAddress(
                        streetAddress = "Geibelstr. 46B",
                        postalCode = "22303",
                        locality = "Hamburg",
                        countryName = "Germany"
                    ),
                    webAddress = CustomCredentialSubject.WebAddress(
                        url = "https://www.delta-dao.com/"
                    ),
                    corporateEmailAddress = "contact@delta-dao.com",
                    individualContactLegal = "legal@delta-dao.com",
                    individualContactTechnical = "support@delta-dao.com",
                    legalForm = "Stock Company",
                    jurisdiction = "Germany",
                    commercialRegister = CustomCredentialSubject.CommercialRegister(
                        organizationName = "Amtsgericht Hamburg (-Mitte)",
                        organizationUnit = "Registergericht",
                        streetAddress = "Caffamacherreihe 20",
                        postalCode = "20355",
                        locality = "Hamburg",
                        countryName = "Germany"
                    ),
                    legalRegistrationNumber = "HRB 170364",
                    ethereumAddress = CustomCredentialSubject.EthereumAddress(
                        id = "0x4C84a36fCDb7Bc750294A7f3B5ad5CA8F74C4A52"
                    ),
                    trustState = "trusted"
                ),
                issuer = "did:example:456",
                proof = Proof(
                    "Ed25519Signature2018",
                    "2020-04-22T10:37:22Z",
                    "assertionMethod",
                    "did:example:456#key-1",
                    "eyJjcml0IjpbImI2NCJdLCJiNjQiOmZhbHNlLCJhbGciOiJFZERTQSJ9..BhWew0x-txcroGjgdtK-yBCqoetg9DD9SgV4245TmXJi-PmqFzux6Cwaph0r-mbqzlE17yLebjfqbRT275U1AA"
                )
            )
        }
    )

    @Json(serializeNull = false)
    override var id: String? = null
}
