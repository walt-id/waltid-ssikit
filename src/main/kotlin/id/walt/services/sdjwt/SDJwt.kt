package id.walt.services.sdjwt

import com.nimbusds.jose.util.Base64URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.text.ParseException

data class SDJwt private constructor (
    val jwt: String,
    val disclosures: Set<String> = setOf(),
    val holderJwt: String? = null,
    val formatForPresentation: Boolean = false
) {
    val undisclosedPayload: JsonObject
        get() = jwt.split(".").get(1).let { Json.parseToJsonElement(Base64URL.from(it).decodeToString()).jsonObject }

    override fun toString(): String {
        return listOf(jwt)
                .plus(disclosures)
                .plus(holderJwt?.let { listOf(it) } ?: (if(formatForPresentation) listOf("") else listOf()))
                .joinToString(SEPARATOR_STR)
    }
    companion object {
        const val DIGESTS_KEY = "_sd"
        const val SEPARATOR = '~'
        const val SEPARATOR_STR = SEPARATOR.toString()
        const val SD_JWT_PATTERN = "^(?<sdjwt>([A-Za-z0-9-_]+)\\.(?<body>[A-Za-z0-9-_]+)\\.([A-Za-z0-9-_]+))(?<disclosures>(~([A-Za-z0-9-_]+))*)(~(?<holderjwt>([A-Za-z0-9-_]+)\\.([A-Za-z0-9-_]+)\\.([A-Za-z0-9-_]+))?)?\$"

        fun fromCombinedSDJwt(combinedSDJwt: String): SDJwt {
            val matchResult = Regex(SD_JWT_PATTERN).matchEntire(combinedSDJwt) ?: throw ParseException("Invalid SD JWT format", 0)
            return SDJwt(
                matchResult.groups["sdjwt"]!!.value,
                matchResult.groups["disclosures"]?.value
                    ?.trim(SEPARATOR)
                    ?.split(SEPARATOR)
                    ?.toSet() ?: setOf(),
                matchResult.groups["holderjwt"]?.value,
                formatForPresentation = matchResult.groups["holderjwt"] != null || combinedSDJwt.endsWith(SEPARATOR)
            )
        }

        fun forIssuance(jwt: String, allDisclosures: Set<String>) = SDJwt(jwt, allDisclosures)

        fun forPresentation(jwt: String, selectedDisclosures: Set<String>, optionalHolderJwt: String? = null)
            = SDJwt(jwt, selectedDisclosures, optionalHolderJwt, true)
    }
}
