package id.walt.model.dif

import com.beust.klaxon.Json
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentation

data class DescriptorMapping(
    val format: String,
    val path: String,
    @Json(serializeNull = false) val id: String? = null,
    @Json(serializeNull = false) val path_nested: DescriptorMapping? = null
) {
    companion object {
        fun vpPath(totalVps: Int, vpIdx: Int) = when (totalVps) {
            0 -> "$"
            1 -> "$"
            else -> "$[$vpIdx]"
        }

        fun vcFormat(vc: VerifiableCredential) = "${
            when (vc.sdJwt) {
                null -> "ldp"
                else -> "jwt"
            }
        }_${
            when (vc) {
                is VerifiablePresentation -> "vp"
                else -> "vc"
            }
        }"

        fun fromVPs(vps: List<VerifiablePresentation>) =
            vps.flatMapIndexed { vpIdx, vp ->
                when (vp.verifiableCredential?.size ?: 0) {
                    // if empty vp, create one Descriptor Mapping with path_nested = null
                    0 -> listOf(
                        DescriptorMapping(
                            id = vpIdx.toString(),
                            format = vcFormat(vp),
                            path = vpPath(vps.size, vpIdx),
                            path_nested = null
                        )
                    )
                    // else, create Descriptor Mapping for each VC in this VP
                    else -> vp.verifiableCredential!!.mapIndexed { vcIdx, vc ->
                        DescriptorMapping(
                            id = vpIdx.toString(),
                            format = vcFormat(vp),
                            path = vpPath(vps.size, vpIdx),
                            path_nested = DescriptorMapping(
                                id = vcIdx.toString(),
                                format = vcFormat(vc),
                                path = "${vpPath(vps.size, vpIdx)}.verifiableCredential[$vcIdx]"
                            )
                        )
                    }
                }
            }
    }
}
