package id.walt.signatory.dataproviders

import id.walt.credentials.w3c.JsonConverter
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.signatory.ProofConfig
import id.walt.signatory.SignatoryDataProvider
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class MergingDataProvider(val partial: Map<String, Any>) : SignatoryDataProvider {

    override fun populate(credentialBuilder: W3CCredentialBuilder, proofConfig: ProofConfig): W3CCredentialBuilder {
        return credentialBuilder.setFromJsonObject(JsonObject(JsonConverter.toJsonElement(partial).jsonObject))
    }
}
