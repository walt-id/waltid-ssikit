package id.walt.services.velocitynetwork.models.responses

import id.walt.services.velocitynetwork.models.LocalizedString
import kotlinx.serialization.Serializable

// TODO: inherit from VerifiableCredential
@Serializable
data class OfferResponse(
    val type: List<String>,
    val credentialSubject: CredentialSubject,
    val offerCreationDate: String, // date
    val offerExpirationDate: String, // date
    val offerId: String,
    val id: String,
    val createdAt: String,
    val updatedAt: String,
    val exchangeId: String,
    val issuer: Issuer,
){
    @Serializable
    data class Issuer(
        val id: String, // did
    )
    @Serializable
    data class CredentialSubject(
        val vendorUserId: String,
        val company: String, // did
        val companyName: LocalizedString,
        val title: LocalizedString,
        val startMonthYear: MonthYear,
        val endMonthYear: MonthYear,
        val location: Location,
    ){
       @Serializable
        data class MonthYear(
            val month: Int,
            val year: Int,
        )

        @Serializable
        data class Location(
            val countryCode: String,
            val regionCode: String,
        )
    }
}
