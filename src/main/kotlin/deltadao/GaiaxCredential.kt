package deltadao

import com.beust.klaxon.Json
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.registry.VerifiableCredentialMetadata

data class GaiaxCredential(
    @Json(name = "@context")
    var context: List<String> = listOf("https://www.w3.org/2018/credentials/v1"),
    override var id: String?,
    var issuer: String,
    @Json(serializeNull = false) var issuanceDate: String? = null,
    @Json(serializeNull = false) var validFrom: String? = null,
    @Json(serializeNull = false) var expirationDate: String? = null,
    @Json(serializeNull = false) var credentialSubject: CustomCredentialSubject,
    @Json(serializeNull = false) var proof: Proof? = null,
) : VerifiableCredential(type) {
    data class CustomCredentialSubject(
        var id: String, // did:key
        var legallyBindingName: String, // deltaDAO AG
        var brandName: String, // deltaDAO
        var legallyBindingAddress: LegallyBindingAddress,
        var webAddress: WebAddress,
        var DNSpublicKey: String, // 04:8B:CA:33:B1:A1:3A:69:E6:A2:1E:BE:CB:4E:DF:75:A9:70:8B:AA:51:83:AB:A1:B0:5A:35:20:3D:B4:29:09:AD:67:B4:12:19:3B:6A:B5:7C:12:3D:C4:CA:DD:A5:E0:DA:05:1E:5E:1A:4B:D1:F2:BA:8F:07:4D:C7:B6:AA:23:46
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

        data class DNSpublicKey(
            var DNSpublicKey: String // 04:8B:CA:33:B1:A1:3A:69:E6:A2:1E:BE:CB:4E:DF:75:A9:70:8B:AA:51:83:AB:A1:B0:5A:35:20:3D:B4:29:09:AD:67:B4:12:19:3B:6A:B5:7C:12:3D:C4:CA:DD:A5:E0:DA:05:1E:5E:1A:4B:D1:F2:BA:8F:07:4D:C7:B6:AA:23:46
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
                id = "did:ebsi-eth:00000001/credentials/1872",
                issuer = "did:example:456",
                issuanceDate = "2020-08-24T14:13:44Z",
                credentialSubject = CustomCredentialSubject(
                    id = "did:key:dummy",
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
                    DNSpublicKey = "04:8B:CA:33:B1:A1:3A:69:E6:A2:1E:BE:CB:4E:DF:75:A9:70:8B:AA:51:83:AB:A1:B0:5A:35:20:3D:B4:29:09:AD:67:B4:12:19:3B:6A:B5:7C:12:3D:C4:CA:DD:A5:E0:DA:05:1E:5E:1A:4B:D1:F2:BA:8F:07:4D:C7:B6:AA:23:46",
                    trustState = "trusted"
                )
            )
        }
    )
}
