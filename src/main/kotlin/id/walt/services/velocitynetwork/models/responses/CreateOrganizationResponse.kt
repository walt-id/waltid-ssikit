package id.walt.services.velocitynetwork.models.responses

import com.beust.klaxon.Json
import id.walt.model.Did
import kotlinx.serialization.Serializable

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