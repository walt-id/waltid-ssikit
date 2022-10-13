package id.walt.model.oidc

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

const val OPENID_CREDENTIAL_DETAILS_TYPE = "openid_credential"

class AuthorizationDetailsTypeAdapter : TypeAdapter<AuthorizationDetails> {
    override fun classFor(type: Any): KClass<out AuthorizationDetails> = when (type.toString()) {
        OPENID_CREDENTIAL_DETAILS_TYPE -> CredentialAuthorizationDetails::class
        else -> AuthorizationDetails::class
    }
}

@Serializable
@TypeFor(field = "type", adapter = AuthorizationDetailsTypeAdapter::class)
open class AuthorizationDetails(
    val type: String
)

class CredentialAuthorizationDetails(
    val credential_type: String,
    val format: String? = null,
    val locations: List<String>? = null
) : AuthorizationDetails(OPENID_CREDENTIAL_DETAILS_TYPE)
