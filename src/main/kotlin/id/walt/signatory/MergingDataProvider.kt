package id.walt.signatory

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.Helpers.toMap
import id.walt.vclib.model.VerifiableCredential
import java.io.StringReader

class MergingDataProvider(val partial: Map<String, Any>) : SignatoryDataProvider {

  fun deepMerge(source: JsonObject, target: JsonObject): JsonObject? {
    for (key in source.keys) {
      val value: Any? = source.get(key)
      if (!target.contains(key)) {
        // new value for "key":
        target.put(key, value)
      } else {
        // existing value for "key" - recursively deep merge:
        if (value is JsonObject) {
          val valueJson: JsonObject = value as JsonObject
          deepMerge(valueJson, target.get(key) as JsonObject)
        } else {
          target.put(key, value)
        }
      }
    }
    return target
  }

  override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
    val defaultDP = DataProviderRegistry.getProvider(template::class)
    var populatedJson = JsonObject(template.toMap())
    var partialJson = Klaxon().parseJsonObject(StringReader(Klaxon().toJsonString(partial)))
    deepMerge(partialJson, populatedJson)
    return defaultDP.populate(populatedJson.toJsonString().toCredential(), proofConfig)
  }
}
