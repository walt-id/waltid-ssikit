package id.walt.credentials.w3c.builder

import id.walt.credentials.w3c.VerifiableCredential
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

interface CredentialFactory<C : VerifiableCredential> {
    fun fromJsonObject(jsonObject: JsonObject): C
    fun fromJson(json: String) = fromJsonObject(Json.parseToJsonElement(json).jsonObject)
}
