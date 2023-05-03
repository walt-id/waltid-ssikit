package id.walt.services.sd_jwt

import id.walt.model.selectiveDisclosure.SDMap
import id.walt.servicematrix.BaseService
import id.walt.services.WaltIdService
import kotlinx.serialization.json.JsonObject

open class SDJwtService: WaltIdService() {
    override val implementation = serviceImplementation<SDJwtService>()

    /**
     * Return combined SD-JWT for issuance with all selective disclosures appended
     */
    open fun sign(
        keyAlias: String, // verification method
        payload: JsonObject, // payload, with all fields disclosed
        sdMap: SDMap        // map indicating for each field, whether it is selectively disclosable
    ): String = implementation.sign(keyAlias, payload, sdMap)

    /**
     * Verifies SD_JWT signature and validity of disclosures
     * @param combinedSdJwt Combined SD_JWT with disclosures
     */
    open fun verify(combinedSdJwt: String): Boolean = implementation.verify(combinedSdJwt)

    /**
     * Returns JsonObject of payload with all disclosed fields recursively resolved
     * @param combinedSdJwt Combined SD_JWT with disclosures
     */
    open fun parsePayload(combinedSdJwt: String): JsonObject
        = implementation.parsePayload(combinedSdJwt)

    /**
     * Returns map indicating for each field, whether it is selectively disclosable
     * @param combinedSdJwt Combined SD_JWT with disclosures
     */
    open fun toSDMap(combinedSdJwt: String): SDMap
        = implementation.toSDMap(combinedSdJwt)

    /**
     * Returns combined sd-jwt with undisclosed (according to sdMap) fields removed from disclosures
     * @param combinedSdJwt Original (as issued) SD-JWT combined with disclosures
     * @param sdMap map indicating, which selectively disclosable fields should be disclosed or undisclosed
     */
    open fun present(combinedSdJwt: String, sdMap: SDMap): String
        = implementation.present(combinedSdJwt, sdMap)
}
