package id.walt.auditor.dynamic

import id.walt.credentials.w3c.JsonConverter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

data class PolicyEngineInput(
    val credentialData: Map<String, Any?>,
    val parameter: Map<String, Any?>?
) {
    fun toJsonObject(): JsonObject {
        return buildJsonObject {
            put("credentialData", JsonConverter.toJsonElement(credentialData))
            put("parameter", JsonConverter.toJsonElement(parameter))
        }
    }

    fun toJson() = toJsonObject().toString()
}
