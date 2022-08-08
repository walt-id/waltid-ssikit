package id.walt.services.velocitynetwork.models.requests

import id.walt.services.velocitynetwork.models.LocalizedString
import kotlinx.serialization.Serializable

@Serializable
data class CredentialOffer(
    val type: List<String>,
    val credentialSubject: CredentialSubject,
    val offerCreationDate: String, // date
    val offerExpirationDate: String, // date
    val offerId: String,
){
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
