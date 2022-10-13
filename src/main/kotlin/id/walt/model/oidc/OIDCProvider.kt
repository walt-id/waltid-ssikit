package id.walt.model.oidc

import com.beust.klaxon.Json
import com.fasterxml.jackson.annotation.JsonInclude
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
open class OIDCProvider(
    val id: String,
    val url: String,
    @Json(serializeNull = false) val description: String? = null,
    @Json(serializeNull = false) val client_id: String? = null,
    @Json(serializeNull = false) val client_secret: String? = null
)

class OIDCProviderWithMetadata(
    id: String,
    url: String,
    description: String? = null,
    client_id: String? = null,
    client_secret: String? = null,
    @Json(ignored = true) val oidc_provider_metadata: OIDCProviderMetadata
) : OIDCProvider(id, url, description, client_id, client_secret)
