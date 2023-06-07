package id.walt.services.oidc

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.nimbusds.oauth2.sdk.AuthorizationRequest
import id.walt.common.KlaxonWithConverters
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.credentials.w3c.toVerifiablePresentation
import id.walt.custodian.Custodian
import id.walt.model.dif.InputDescriptor
import id.walt.model.dif.PresentationDefinition
import id.walt.model.oidc.VCClaims
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.*

object OIDCUtils {
    fun getVCClaims(authRequest: AuthorizationRequest): VCClaims {
        val claims =
            (authRequest.requestObject?.jwtClaimsSet?.claims?.get("claims")?.toString()
                ?: authRequest.customParameters["claims"]?.firstOrNull())
                ?.let { JSONParser(-1).parse(it) as JSONObject }
                ?.let {
                    when (it.containsKey("vp_token") || it.containsKey("credentials")) {
                        true -> it.toJSONString()
                        else -> it["id_token"]?.toString() // EBSI WCT: vp_token is wrongly (?) contained inside id_token object
                    }
                }
                ?.let { KlaxonWithConverters().parse<VCClaims>(it) } ?: VCClaims()
        return claims
    }

    fun getCodeFromRedirectUri(redirectUri: URI): String? {
        return Pattern.compile("&")
            .split(redirectUri.query)
            .map { s -> s.split(Pattern.compile("="), 2) }.associate { o ->
                Pair(o[0].let { URLDecoder.decode(it, StandardCharsets.UTF_8) },
                    o[1].let { URLDecoder.decode(it, StandardCharsets.UTF_8) })
            }["code"]
    }

    fun toVpToken(vps: List<VerifiablePresentation>): String =
        when (vps.size) {
            1 -> vps[0].encode()
            else -> {
                vps.joinToString(",", "[", "]") { vp ->
                    vp.sdJwt?.let { "\"$it\"" } ?: vp.encode()
                }
            }
        }

    fun fromVpToken(vp_token: String): List<VerifiablePresentation> {
        if (vp_token.trim().startsWith('[')) {
            return Json.parseToJsonElement(vp_token).jsonArray.map {
                when (it) {
                    is kotlinx.serialization.json.JsonObject -> it.jsonObject.toString()
                    else -> it.jsonPrimitive.content
                }
            }.map { it.toVerifiablePresentation() }
        } else {
            return listOf(vp_token.toVerifiablePresentation())
        }
    }

    fun matchSingleJsonField(fieldValue: Any?, fieldFilter: Map<String, Any?>?): Boolean {
        return if (fieldFilter?.containsKey("pattern") == true) {
            Regex(fieldFilter["pattern"].toString()).matches(fieldValue.toString())
        } else if (fieldFilter?.containsKey("const") == true) {
            fieldFilter["const"] == fieldValue
        } else {
            false
        }
    }

    fun matchesInputDescriptor(credential: VerifiableCredential, inputDescriptor: InputDescriptor): Boolean {
        // for now: support
        // * schema.uri from presentation exchange 1.0
        // * field constraints from presentation exchange 2.0:
        // ** paths: "$.type", "$.credentialSchema.id"
        // ** match type: pattern, const

        if (inputDescriptor.schema != null) { // PEX 1.0
            return inputDescriptor.schema.uri == credential.credentialSchema?.id
        } else { // PEX 2.0
            return inputDescriptor.constraints?.fields?.any { fld ->
                var isSingleFieldValue = false
                val fldVal = if (fld.path.any { it.matches(Regex("\\\$(\\.vc)?\\.type")) }) {
                    isSingleFieldValue = true
                    credential.type.last()
                } else if (fld.path.any { it.matches(Regex("\\\$(\\.vc)?\\.credentialSchema.id")) }) {
                    isSingleFieldValue = true
                    credential.credentialSchema?.id
                } else if (fld.path.any { it.matches(Regex("\\\$(\\.vc)?\\.credentialSchema")) }) {
                    credential.credentialSchema?.toJsonObject()
                } else {
                    null
                }
                return fldVal?.let {
                    return if (isSingleFieldValue) {
                        matchSingleJsonField(fldVal, fld.filter)
                    } else if (fld.filter?.containsKey("allOf") == true) {
                        (fld.filter["allOf"] as JsonArray<JsonObject>).any { allOf ->
                            allOf.containsKey("contains") && (allOf["contains"] as JsonObject).let { contains ->
                                contains.containsKey("properties") && (contains["properties"] as JsonObject).let { properties ->
                                    properties.keys.all { key ->
                                        matchSingleJsonField(
                                            (fldVal as kotlinx.serialization.json.JsonObject)[key],
                                            properties[key] as Map<String, Any?>?
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        false
                    }
                } ?: false
            } ?: false
        }
    }

    /**
     * Find credentials matching input descriptors in presentation definition. optionally filter by subject DID
     * @return Map from input_descriptor id to set of matching credential ids
     */
    fun findCredentialsFor(presentationDefinition: PresentationDefinition, subject: String? = null): Map<String, Set<String>> {

        val myCredentials = Custodian.getService().listCredentials()
        return presentationDefinition.input_descriptors.associate { indesc ->
            Pair(indesc.id, myCredentials.filter { c ->
                matchesInputDescriptor(c, indesc) &&
                        (subject == null || subject == c.subjectId)
            }.map { c -> c.id!! }.toSet())
        }
    }
}
