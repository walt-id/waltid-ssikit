package id.walt.services.velocitynetwork.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class OfferResponse(
    val type: List<String>,
    val issuer: Issuer,
    val offerCreationDate: String, // date
    val offerExpirationDate: String, // date
    val offerId: String,
    val id: String,
    val exchangeId: String,
    val hash: String,
    val credentialSubject: CredentialSubject,
){
    @Serializable
    data class Issuer(
        val id: String, // did
    )
    @Serializable
    data class CredentialSubject(
        val company: String, // did
        val companyName: LocalizedString,
        val title: LocalizedString,
        val startMonthYear: MonthYear,
        val endMonthYear: MonthYear,
        val location: Location,
    ){
        @Serializable
        data class LocalizedString(
            val localized: En,
        ){
            @Serializable
            data class En(
                val en: String,
            )
        }

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
