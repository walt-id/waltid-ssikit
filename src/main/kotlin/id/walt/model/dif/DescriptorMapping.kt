package id.walt.model.dif

import com.beust.klaxon.Json
import id.walt.vclib.model.VerifiableCredential

data class DescriptorMapping (
    @Json(serializeNull = false) val id: String?,
    val format: String,
    val path: String,
    @Json(serializeNull = false) val path_nested: DescriptorMapping?
) {
    companion object {
        fun fromVP(id: String, vpStr: String) = DescriptorMapping(
            id = id,
            format = when(VerifiableCredential.isJWT(vpStr)) {
                true -> "jwt_vp"
                else -> "ldp_vp"
            },
            path = "$",
            path_nested = null
        )
    }
}
