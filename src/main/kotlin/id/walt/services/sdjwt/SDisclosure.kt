package id.walt.services.sdjwt

import com.nimbusds.jose.util.Base64URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.text.ParseException

data class SDisclosure private constructor (
    val disclosure: String,
    val salt: String,
    val key: String,
    val value: JsonElement
) {
    companion object {
        fun parse(disclosure: String) = Json.parseToJsonElement(Base64URL.from(disclosure).decodeToString()).jsonArray.let {
            if (it.size != 3) {
                throw ParseException("Invalid disclosure", 0)
            }
            SDisclosure(disclosure, it[0].jsonPrimitive.content, it[1].jsonPrimitive.content, it[2])
        }
    }
}
