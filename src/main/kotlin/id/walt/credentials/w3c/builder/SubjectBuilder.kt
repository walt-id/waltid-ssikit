package id.walt.credentials.w3c.builder

import id.walt.credentials.w3c.W3CCredentialSubject
import kotlinx.serialization.json.JsonObject

class SubjectBuilder : BasicBuilder<W3CCredentialSubject, SubjectBuilder>() {
    fun setId(id: String?) = setProperty("id", id)

    override fun build(): W3CCredentialSubject {
        return W3CCredentialSubject.fromJsonObject(JsonObject(properties))
    }
}
