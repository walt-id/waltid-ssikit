package id.walt.services.sdjwt

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.util.Base64URL
import id.walt.credentials.selectiveDisclosure.SDField
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import kotlinx.serialization.json.*
import mu.KotlinLogging
import org.sd_jwt.SdJwtHeader
import org.sd_jwt.createCredential
import java.security.MessageDigest
import java.security.SecureRandom

open class WaltIdSDJwtService: SDJwtService() {
    private val log = KotlinLogging.logger {}
    open val keyService = KeyService.getService()
    private fun digest(value: String): String {
        val hashFunction = MessageDigest.getInstance("SHA-256")
        val messageDigest = hashFunction.digest(value.toByteArray(Charsets.UTF_8))
        return Base64URL.encode(messageDigest).toString()
    }

    private fun generateSalt(): String {
        val secureRandom = SecureRandom()
        val randomness = ByteArray(16)
        secureRandom.nextBytes(randomness)
        return Base64URL.encode(randomness).toString()
    }

    private fun generateDisclosure(key: String, value: JsonElement): String {
        return Base64URL.encode(buildJsonArray {
            add(generateSalt())
            add(key)
            add(value)
        }.toString()).toString()
    }

    private fun digestSDClaim(key: String, value: JsonElement, disclosures: MutableSet<String>): String {
        return digest(generateDisclosure(key, value).also {
            disclosures.add(it)
        })
    }

    private fun removeSDFields(payload: JsonObject, sdMap: Map<String, SDField>): JsonObject {
        return JsonObject(payload.filterKeys { key -> sdMap[key]?.sd != true }.mapValues { entry ->
            if(entry.value is JsonObject && !sdMap[entry.key]?.nestedMap.isNullOrEmpty()) {
                removeSDFields(entry.value.jsonObject, sdMap[entry.key]?.nestedMap ?: mapOf())
            } else {
                entry.value
            }
        })
    }

    fun generateSDPayload(payload: JsonObject, sdMap: Map<String, SDField>, disclosures: MutableSet<String>): JsonObject {
        val sdPayload = removeSDFields(payload, sdMap).toMutableMap()
        val digests = payload.filterKeys { key ->
                // iterate over all fields that are selectively disclosable AND/OR have nested fields that might be:
                sdMap[key]?.sd == true || !sdMap[key]?.nestedMap.isNullOrEmpty()
            }.map { entry ->
                if(entry.value !is JsonObject || sdMap[entry.key]?.nestedMap.isNullOrEmpty()) {
                    // this field has no nested elements and/or is selectively disclosable only as a whole:
                    digestSDClaim(entry.key, entry.value, disclosures)
                } else {
                    // the nested properties could be selectively disclosable individually
                    // recursively generate SD payload for nested object:
                    val nestedSDPayload = generateSDPayload(entry.value.jsonObject, sdMap[entry.key]!!.nestedMap!!, disclosures)
                    if(sdMap[entry.key]?.sd == true) {
                        // this nested object is also selectively disclosable as a whole
                        // so let's compute the digest and disclosure for the nested SD payload:
                        digestSDClaim(entry.key, nestedSDPayload, disclosures)
                    } else {
                        // this nested object can is not selectively disclosable as a whole, add the nested SD payload as it is:
                        sdPayload[entry.key] = nestedSDPayload
                        // no digest/disclosure is added for this field (though the nested properties may have generated digests and disclosures)
                        null
                    }
                }
            }.filterNotNull().toSet()

        if(digests.isNotEmpty()) {
            sdPayload.put(DIGESTS_KEY, buildJsonArray {
                digests.forEach { add(it) }
            })
        }
        return JsonObject(sdPayload)
    }

    override fun sign(keyAlias: String, payload: JsonObject, sdMap: Map<String, SDField>): String {
        val disclosures = mutableSetOf<String>()
        val sdPayload = generateSDPayload(payload, sdMap, disclosures)
        val sdJwt = JwtService.getService().sign(keyAlias, sdPayload.toString())
        return listOf(sdJwt).plus(disclosures).joinToString(DISCLOSURES_SEPARATOR)
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

    companion object {
        val DIGESTS_KEY = "_sd"
        val DISCLOSURES_SEPARATOR = "~"
    }
}
