package id.walt.signatory.dataproviders

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import id.walt.signatory.ProofConfig
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential
import java.io.StringReader

class MergingDataProvider(val partial: Map<String, Any>) : AbstractDataProvider<VerifiableCredential>() {

    fun deepMerge(source: JsonObject, target: JsonObject): JsonObject {
        for (key in source.keys) {
            val value: Any? = source[key]
            if (!target.contains(key)) {
                // new value for "key":
                target.put(key, value)
            } else {
                // existing value for "key" - recursively deep merge:
                if (value is JsonObject) {
                    val valueJson: JsonObject = value
                    deepMerge(valueJson, target[key] as JsonObject)
                } else {
                    target[key] = value
                }
            }
        }
        return target
    }

    override fun populateCustomData(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
        var populatedJson = JsonObject(template.toMap())
        var partialJson = Klaxon().parseJsonObject(StringReader(Klaxon().toJsonString(partial)))
        deepMerge(partialJson, populatedJson)
        return populatedJson.toJsonString().toCredential()
    }
}
