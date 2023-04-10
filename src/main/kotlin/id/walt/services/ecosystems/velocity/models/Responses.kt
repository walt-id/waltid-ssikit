package id.walt.services.ecosystems.velocity.models

import com.beust.klaxon.Json
import id.walt.model.Did
import kotlinx.serialization.Serializable

@Serializable
data class CompleteOfferResponse(
    val offerIds: List<String>,
)

data class CreateDisclosureResponse(
    val id: String,
    val description: String,
    val types: List<String>,
    val purpose: String,
    val duration: String,
    val vendorEndpoint: String,
    val termsUrl: String,
    val vendorDisclosureId: String,
    val activationDate: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CreateOrganizationResponse(
    val id: String,
    val didDoc: Did,
    val profile: Profile,
    val keys: List<Key>,
    val authClients: List<AuthClient>,
    val ids: Ids,
) {

    @Serializable
    data class Profile(
        val name: String,
        val location: Location,
        val logo: String,
        val founded: String,
        @Json(serializeNull = false)
        val closed: String? = null,
        val type: String,
        val id: String,
        val verifiableCredentialJwt: String,
        val permittedVelocityServiceCategory: ArrayList<String>
    ){
        @Serializable
        data class Location(
            val countryCode: String,
            val regionCode: String,
        )
    }

    @Serializable
    data class AuthClient(
        val type: String,
        val clientType: String,
        val clientId: String,
        val clientSecret: String,
        val serviceId: String,
    )

    @Serializable
    data class Key(
        val id: String,
        val purposes: List<String>,
        val key: String,
        val publicKey: String,
        val algorithm: String,
        val encoding: String,
        val controller: String,
    )

    @Serializable
    data class Ids(
        val tokenAccountId: String,
        val fineractClientId: String,
        val escrowAccountId: String,
        val ethereumAccount: String,
        val did: String,
    )
}

@Serializable
data class DisclosureResponse(
    val token: String
)

@Serializable
data class ExchangeResponse(
    val id: String
)

@Serializable
data class InspectionResult(
    val credentials: List<Credential>,
){
    @Serializable
    data class Credential(
        val credentialChecks: Map<CredentialCheckType, CredentialCheckValue>
    )
}

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

@Serializable
data class HolderExchangeResponse(
    val exchangeId: String
)

@Serializable
data class IssuerExchangeResponse(
    val id: String
)
