package id.walt.signatory.dataproviders

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import id.walt.common.deepMerge
import id.walt.signatory.ProofConfig
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential
import java.io.StringReader

class MergingDataProvider(val partial: Map<String, Any>) : AbstractDataProvider<VerifiableCredential>() {

    override fun populateCustomData(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
        var populatedJson = JsonObject(template.toMap())
        var partialJson = Klaxon().parseJsonObject(StringReader(Klaxon().toJsonString(partial)))
        populatedJson.deepMerge(partialJson)
        return populatedJson.toJsonString().toCredential()
    }
}
