package id.walt.model.oidc

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class IssuanceInitiationRequest(
    val issuer_url: String,
    val credential_types: List<String>,
    val pre_authorized_code: String? = null,
    val user_pin_required: Boolean = false,
    val op_state: String? = null
) {

    val isPreAuthorized
        get() = pre_authorized_code != null

    fun toQueryParams(): Map<String, List<String>> {
        return buildMap {
            put("issuer", listOf(issuer_url))
            put("credential_type", credential_types)
            pre_authorized_code?.let { put("pre-authorized_code", listOf(it)) }
            put("user_pin_required", listOf(user_pin_required.toString()))
            op_state?.let { put("op_state", listOf(it)) }
        }
    }

    fun toQueryString(): String {
        return toQueryParams().flatMap { (key, value) ->
            value.map {
                "${key}=${
                    URLEncoder.encode(
                        it,
                        StandardCharsets.UTF_8
                    )
                }"
            }
        }.joinToString("&")
    }

    companion object {
        fun fromQueryParams(params: Map<String, List<String>>): IssuanceInitiationRequest {
            return IssuanceInitiationRequest(
                issuer_url = params["issuer"]?.firstOrNull() ?: throw IllegalArgumentException("Missing parameter 'issuer'"),
                credential_types = params["credential_type"] ?: throw IllegalArgumentException("Missing parameter(s) 'credential_type'"),
                pre_authorized_code = params["pre-authorized_code"]?.firstOrNull(),
                user_pin_required = params["user_pin_required"]?.map { it.toBoolean() }?.firstOrNull() ?: false,
                op_state = params["op_state"]?.firstOrNull()
            )
        }
    }
}
