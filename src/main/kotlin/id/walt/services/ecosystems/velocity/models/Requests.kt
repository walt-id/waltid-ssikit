package id.walt.services.ecosystems.velocity.models

import kotlinx.serialization.Serializable

@Serializable
data class CheckCredentialRequest(
    val rawCredentials: List<RawCredential>,
) {
    @Serializable
    data class RawCredential(
        val rawCredential: String,
    )
}

@Serializable
data class CreateDisclosureRequest(
    val types: List<Type>,
    val vendorEndpoint: String,
    val vendorDisclosureId: String,
    val purpose: String,
    val duration: String,
    val termsUrl: String,
    val activationDate: String,
){
    @Serializable
    data class Type(
        val type: String
    )
}

@Serializable
data class CreateTenantRequest(
    val serviceIds: List<String>,
    val did: String,
    val keys: List<Key>,
){
    @Serializable
    data class Key(
        val purposes: List<String>,
        val algorithm: String,
        val encoding: String,
        val kidFragment: String,
        val key: String,
    )
}

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

@Serializable
data class DisclosureRequestBody(
    val exchangeId: String,
    val presentation: String,
)

@Serializable
data class ExchangeRequestBody(
    val type: String
)

@Serializable
data class FinalizeOfferRequestBody(
    val exchangeId: String,
    val approvedOfferIds: List<String>,
    val rejectedOfferIds: List<String>
)

@Serializable
data class GetOffersRequestBody(
    val exchangeId: String,
    val credentialTypes: List<String>,
)

@Serializable
data class HolderExchangeRequestBody(
    val type: String
)

@Serializable
data class IssuerExchangeRequestBody(
    val type: String,
    val disclosureId: String,
    val identityMatcherValues: List<String>
)

