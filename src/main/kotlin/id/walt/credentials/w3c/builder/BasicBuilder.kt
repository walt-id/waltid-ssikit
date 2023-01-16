package id.walt.credentials.w3c.builder

import id.walt.credentials.w3c.ICredentialElement
import id.walt.credentials.w3c.JsonConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

abstract class BasicBuilder<T : ICredentialElement, B : BasicBuilder<T, B>> {
    protected val properties = mutableMapOf<String, JsonElement>()

    private fun setAnyProperty(key: String, value: Any?): B {
        properties[key] = JsonConverter.toJsonElement(value)
        return this as B
    }

    fun setProperty(key: String, value: Boolean?) = setAnyProperty(key, value)
    fun setProperty(key: String, value: Number?) = setAnyProperty(key, value)
    fun setProperty(key: String, value: String?) = setAnyProperty(key, value)
    fun setProperty(key: String, value: Map<String, Any?>?) = setAnyProperty(key, value)
    fun setProperty(key: String, value: List<Any?>?) = setAnyProperty(key, value)
    fun setProperty(key: String, value: JsonElement) = setAnyProperty(key, value)

    open fun setFromJsonObject(jsonObject: JsonObject): B {
        properties.putAll(jsonObject)
        return this as B
    }

    fun setFromJson(json: String) = setFromJsonObject(Json.parseToJsonElement(json).jsonObject)

    abstract fun build(): T
}
