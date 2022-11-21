package id.walt.credentials.w3c.builder

import id.walt.credentials.w3c.W3CCredential
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

interface CredentialFactory<C: W3CCredential> {
  fun fromJsonObject(jsonObject: JsonObject): C
  fun fromJson(json: String) = fromJsonObject(Json.parseToJsonElement(json).jsonObject)
}
