package id.walt.services.sdjwt

import com.nimbusds.jose.JOSEObjectType
import id.walt.credentials.selectiveDisclosure.SDField
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import org.sd_jwt.SdJwtHeader
import org.sd_jwt.createCredential
import org.sd_jwt.parseDisclosures
import org.sd_jwt.verifyPresentation

open class WaltIdSDJwtService: SDJwtService() {
    private val log = KotlinLogging.logger {}
    open val keyService = KeyService.getService()
    private fun generateDiscloseStructure(fullObject: JsonObject, sdMap: Map<String, SDField>): JsonObject {
        return JsonObject(fullObject.filterKeys { key -> !sdMap.containsKey(key) || sdMap[key]!!.isDisclosed }.mapValues { entry ->
            if(entry.value is JsonObject && sdMap.containsKey(entry.key) && !sdMap[entry.key]!!.nestedMap.isNullOrEmpty()) {
                generateDiscloseStructure(entry.value.jsonObject, (sdMap[entry.key]!!.nestedMap ?: mapOf()))
            } else {
                entry.value
            }
        })
    }

    private fun generateDisclosure(simpleField: JsonElement): String {
        throw TODO()
    }

    private fun fillUndisclosedFieldsAndGenerateDisclosures(fullObject: JsonObject, disclosureStructure: JsonObject, sdMap: Map<String, SDField>): Set<String> {
        if(!sdMap.keys.containsAll(fullObject.keys)) {
            throw Exception("sdMap doesn't contain entry for each object field")
        }
        return fullObject.filterKeys { key -> !sdMap[key]!!.isDisclosed }.flatMap { undisclosedEntry ->
            if(undisclosedEntry.value is JsonObject && !sdMap[undisclosedEntry.key]!!.nestedMap.isNullOrEmpty()) {
                throw TODO() //fillUndisclosedFieldsAndGenerateDisclosures(undisclosedEntry.value.jsonObject, )
            } else {
                setOf(generateDisclosure(undisclosedEntry.value))
            }
        }.toSet()
    }

    override fun sign(keyAlias: String, payload: JsonObject, sdMap: Map<String, SDField>): String {
        if (!keyService.hasKey(keyAlias)) {
            log.error { "Could not load signing key for $keyAlias" }
            throw IllegalArgumentException("Could not load signing key for $keyAlias")
        }
        return createCredential(payload, keyService.toJwk(keyAlias, KeyType.PRIVATE), null, generateDiscloseStructure(payload, sdMap), SdJwtHeader(
            JOSEObjectType("vc+sd-jwt"), "credential-claims-set+json"
        ), true)
    }

    override fun toSDMap(combinedSdJwt: String): Map<String, SDField> {
        throw TODO()
    }

    override fun parsePayload(combinedSdJwt: String): JsonObject {
        throw TODO()
    }

    override fun verify(combinedSdJwt: String): Boolean {
        throw TODO()
    }

    override fun present(combinedSdJwt: String, sdMap: Map<String, SDField>): String {
        throw TODO()
    }
}
