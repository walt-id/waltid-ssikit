package id.walt.services.sdjwt

import com.nimbusds.jose.util.Base64URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.text.ParseException

data class SDJwt (
    val jwt: String,
    val sdPayload: JsonObject,
    val digests2Disclosures: Map<String, SDisclosure> = mapOf(),
    val holderJwt: String? = null,
    val formatForPresentation: Boolean = false
) {
    val disclosures
        get() = digests2Disclosures.values.toSet()

    override fun toString(): String {
        return listOf(jwt)
                .plus(disclosures.map { it.disclosure })
                .plus(holderJwt?.let { listOf(it) } ?: (if(formatForPresentation) listOf("") else listOf()))
                .joinToString(SEPARATOR_STR)
    }

    companion object {
        const val DIGESTS_KEY = "_sd"
        const val SEPARATOR = '~'
        const val SEPARATOR_STR = SEPARATOR.toString()
        const val SD_JWT_PATTERN = "^(?<sdjwt>([A-Za-z0-9-_]+)\\.(?<body>[A-Za-z0-9-_]+)\\.([A-Za-z0-9-_]+))(?<disclosures>(~([A-Za-z0-9-_]+))+)?(~(?<holderjwt>([A-Za-z0-9-_]+)\\.([A-Za-z0-9-_]+)\\.([A-Za-z0-9-_]+))?)?\$"
    }
}
