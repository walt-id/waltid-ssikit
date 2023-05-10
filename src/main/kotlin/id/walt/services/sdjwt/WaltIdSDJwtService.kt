package id.walt.services.sdjwt

import com.nimbusds.jose.util.Base64URL
import id.walt.credentials.selectiveDisclosure.SDField
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.ParseException

open class WaltIdSDJwtService: SDJwtService() {
    private val log = KotlinLogging.logger {}
    open val keyService = KeyService.getService()

    override fun parseSDJwt(combinedSDJwt: String): SDJwt {
        val matchResult = Regex(SDJwt.SD_JWT_PATTERN).matchEntire(combinedSDJwt) ?: throw ParseException("Invalid SD JWT format", 0)
        val disclosures = matchResult.groups["disclosures"]?.value
            ?.trim(SDJwt.SEPARATOR)
            ?.split(SDJwt.SEPARATOR)
            ?.toSet() ?: setOf()
        return SDJwt(
            matchResult.groups["sdjwt"]!!.value,
            combinedSDJwt.split(".").get(1).let { Json.parseToJsonElement(Base64URL.from(it).decodeToString()).jsonObject },
            disclosures.associate { Pair(digest(it), SDisclosure.parse(it)) },
            matchResult.groups["holderjwt"]?.value,
            formatForPresentation = matchResult.groups["holderjwt"] != null || combinedSDJwt.endsWith(SDJwt.SEPARATOR)
        )
    }
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

    private fun generateDisclosure(key: String, value: JsonElement): SDisclosure {
        val salt = generateSalt()
        return Base64URL.encode(buildJsonArray {
            add(salt)
            add(key)
            add(value)
        }.toString()).toString().let {disclosure ->
            SDisclosure(disclosure, salt, key, value)
        }
    }

    private fun digestSDClaim(key: String, value: JsonElement, digests2disclosures: MutableMap<String, SDisclosure>): String {
        val disclosure = generateDisclosure(key, value)
        return digest(disclosure.disclosure).also {
            digests2disclosures[it] = disclosure
        }
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

    fun generateSDPayload(payload: JsonObject, sdMap: Map<String, SDField>, digests2disclosures: MutableMap<String, SDisclosure>): JsonObject {
        val sdPayload = removeSDFields(payload, sdMap).toMutableMap()
        val digests = payload.filterKeys { key ->
                // iterate over all fields that are selectively disclosable AND/OR have nested fields that might be:
                sdMap[key]?.sd == true || !sdMap[key]?.nestedMap.isNullOrEmpty()
            }.map { entry ->
                if(entry.value !is JsonObject || sdMap[entry.key]?.nestedMap.isNullOrEmpty()) {
                    // this field has no nested elements and/or is selectively disclosable only as a whole:
                    digestSDClaim(entry.key, entry.value, digests2disclosures)
                } else {
                    // the nested properties could be selectively disclosable individually
                    // recursively generate SD payload for nested object:
                    val nestedSDPayload = generateSDPayload(entry.value.jsonObject, sdMap[entry.key]!!.nestedMap!!, digests2disclosures)
                    if(sdMap[entry.key]?.sd == true) {
                        // this nested object is also selectively disclosable as a whole
                        // so let's compute the digest and disclosure for the nested SD payload:
                        digestSDClaim(entry.key, nestedSDPayload, digests2disclosures)
                    } else {
                        // this nested object is not selectively disclosable as a whole, add the nested SD payload as it is:
                        sdPayload[entry.key] = nestedSDPayload
                        // no digest/disclosure is added for this field (though the nested properties may have generated digests and disclosures)
                        null
                    }
                }
            }.filterNotNull().toSet()

        if(digests.isNotEmpty()) {
            sdPayload.put(SDJwt.DIGESTS_KEY, buildJsonArray {
                digests.forEach { add(it) }
            })
        }
        return JsonObject(sdPayload)
    }

    override fun sign(keyAlias: String, payload: JsonObject, sdMap: Map<String, SDField>?): SDJwt {
        val digests2Disclosures = mutableMapOf<String, SDisclosure>()
        val sdPayload = sdMap?.let {
            generateSDPayload(payload, it, digests2Disclosures)
        } ?: payload
        val sdJwt = JwtService.getService().sign(keyAlias, sdPayload.toString())
        return SDJwt(sdJwt, sdPayload, digests2Disclosures.toMap())
    }

    private fun unveilDislosureIfPresent(digest: String, digests2Disclosures: MutableMap<String, SDisclosure>, objectBuilder: JsonObjectBuilder) {
        val disclosure = digests2Disclosures.remove(digest)
        if(disclosure != null) {
            objectBuilder.put(disclosure.key,
                if(disclosure.value is JsonObject) {
                    resolveDislosedPayload(disclosure.value.jsonObject, digests2Disclosures)
                } else disclosure.value
            )
        }
    }

    private fun resolveDislosedPayload(payload: JsonObject, digests2Disclosures: MutableMap<String, SDisclosure>): JsonObject {
        return buildJsonObject {
            payload.forEach { key, value ->
                if(key == SDJwt.DIGESTS_KEY) {
                    if(value !is JsonArray) throw ParseException("SD-JWT contains invalid ${SDJwt.DIGESTS_KEY} element", 0)
                    value.jsonArray.forEach {
                        unveilDislosureIfPresent(it.jsonPrimitive.content, digests2Disclosures, this)
                    }
                } else if(value is JsonObject) {
                    put(key, resolveDislosedPayload(value.jsonObject, digests2Disclosures))
                } else {
                    put(key, value)
                }
            }
        }
    }

    override fun disclosePayload(sdJwt: SDJwt): JsonObject {
        val digests2Disclosures = sdJwt.digests2Disclosures.toMutableMap()
        return resolveDislosedPayload(sdJwt.sdPayload, digests2Disclosures)
    }

    private fun verifyDisclosuresInPayload(sdJwt: SDJwt): Boolean {
        val digests2Disclosures = sdJwt.digests2Disclosures.toMutableMap()
        resolveDislosedPayload(sdJwt.sdPayload, digests2Disclosures)
        return digests2Disclosures.isEmpty()
    }

    override fun verify(sdJwt: SDJwt): Boolean {
        return  JwtService.getService().verify(sdJwt.jwt) &&
                verifyDisclosuresInPayload(sdJwt) &&
                (sdJwt.holderJwt?.let { JwtService.getService().verify(it) } ?: true)
    }

    private fun selectDisclosures(payload: JsonObject, sdMap: Map<String, SDField>, digests2Disclosures: Map<String, SDisclosure>): Set<String> {
        if(!payload.containsKey(SDJwt.DIGESTS_KEY) || payload[SDJwt.DIGESTS_KEY] !is JsonArray) {
            throw Exception("No selectively disclosable fields found in given JWT payload, or invalid ${SDJwt.DIGESTS_KEY} format found")
        }

        return payload[SDJwt.DIGESTS_KEY]!!.jsonArray
            .map { it.jsonPrimitive.content }
            .filter { digest -> digests2Disclosures.containsKey(digest) }
            .map { digest -> digests2Disclosures[digest]!! }
            .filter {sd -> sdMap[sd.key]?.sd == true }
            .flatMap { sd ->
                listOf(sd.disclosure).plus(
                    if(sd.value is JsonObject && !sdMap[sd.key]?.nestedMap.isNullOrEmpty()) {
                        selectDisclosures(sd.value, sdMap[sd.key]!!.nestedMap!!, digests2Disclosures)
                    } else listOf()
                )
            }.toSet()
    }

    override fun present(sdJwt: SDJwt, sdMap: Map<String, SDField>?): SDJwt {
        val selectedDisclosures = sdMap?.let {
            selectDisclosures(sdJwt.sdPayload, it, sdJwt.digests2Disclosures)
        } ?: sdJwt.disclosures
        return SDJwt(sdJwt.jwt, sdJwt.sdPayload, sdJwt.digests2Disclosures.filterValues { selectedDisclosures.contains(it.disclosure) }, formatForPresentation = true)
    }

    private fun createSDFieldFor(sd: Boolean, key: String, value: JsonElement, digests2Disclosures: Map<String, SDisclosure>): SDField {
        return SDField(sd, nestedMap = if(value is JsonObject) {
            createSdMapFor(value.jsonObject, digests2Disclosures)
        } else null)
    }

    private fun createSdMapFor(payload: JsonObject, digests2Disclosures: Map<String, SDisclosure>): Map<String, SDField> {
        if(!payload.containsKey(SDJwt.DIGESTS_KEY) || payload[SDJwt.DIGESTS_KEY] !is JsonArray) {
            return mapOf()
        }

        return payload[SDJwt.DIGESTS_KEY]!!.jsonArray
            .map { it.jsonPrimitive.content }
            .filter { digest -> digests2Disclosures.containsKey(digest) }
            .map { digest -> digests2Disclosures[digest]!! }
            .associate { sd ->
                Pair(sd.key, createSDFieldFor(true, sd.key, sd.value, digests2Disclosures))
            }
            .plus(payload.filter { entry -> entry.key != SDJwt.DIGESTS_KEY }.mapValues { entry ->
                createSDFieldFor(false, entry.key, entry.value, digests2Disclosures)
            })
    }

    override fun toSDMap(sdJwt: SDJwt): Map<String, SDField> {
        return createSdMapFor(sdJwt.sdPayload, sdJwt.digests2Disclosures)
    }
}
