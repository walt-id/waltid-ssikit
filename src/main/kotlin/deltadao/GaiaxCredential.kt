package deltadao

import com.beust.klaxon.Json
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.registry.VerifiableCredentialMetadata

data class GaiaxCredential(
    @Json(name = "@context")
    var context: List<String> = listOf("https://www.w3.org/2018/credentials/v1"),
    @Json(serializeNull = false) var credentialSubject: CustomCredentialSubject,
    @Json(serializeNull = false) var issuer: String? = null,
    @Json(serializeNull = false) var proof: Proof? = null,
) : VerifiableCredential(type) {
    data class CustomCredentialSubject(
        var legallyBindingName: String, // deltaDAO AG
        var brandName: String, // deltaDAO
        var legallyBindingAddress: LegallyBindingAddress,
        var webAddress: WebAddress,
        var corporateEmailAddress: String, // contact@delta-dao.com
        var individualContactLegal: String, // legal@delta-dao.com
        var individualContactTechnical: String, // support@delta-dao.com
        var legalForm: String, // Stock Company
        var jurisdiction: String, // Germany
        var commercialRegister: CommercialRegister,
        var legalRegistrationNumber: String, // HRB 170364
        var ethereumAddress: EthereumAddress,
        var trustState: String // trusted
    ) {
        data class LegallyBindingAddress(
            var streetAddress: String, // Geibelstr. 46B
            var postalCode: String, // 22303
            var locality: String, // Hamburg
            var countryName: String // Germany
        )

        data class WebAddress(
            var url: String // https://www.delta-dao.com/
        )

        data class CommercialRegister(
            var organizationName: String, // Amtsgericht Hamburg (-Mitte)
            var organizationUnit: String, // Registergericht
            var streetAddress: String, // Caffamacherreihe 20
            var postalCode: String, // 20355
            var locality: String, // Hamburg
            var countryName: String // Germany
        )

        data class EthereumAddress(
            var id: String // 0x4C84a36fCDb7Bc750294A7f3B5ad5CA8F74C4A52
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
