package id.walt.model.dif

import com.beust.klaxon.Json
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.VerifiableCredential

data class DescriptorMapping (
    val format: String,
    val path: String,
    @Json(serializeNull = false) val id: String? = null,
    @Json(serializeNull = false) val path_nested: DescriptorMapping? = null
) {
    companion object {
        fun fromVP(vp: VerifiablePresentation, id: String? = null) = DescriptorMapping(
            id = id,
            format = when(vp.jwt != null) {
                true -> "jwt_vp"
                else -> "ldp_vp"
            },
            path = "$",
            path_nested = null
        )
    }
}
