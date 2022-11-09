package id.walt.model.dif

import com.beust.klaxon.JsonObject
import id.walt.model.oidc.JsonObjectField

data class InputDescriptorField(
    val path: List<String>,
    val id: String? = null,
    val purpose: String? = null,
    @JsonObjectField val filter: Map<String, Any?>? = null
)
