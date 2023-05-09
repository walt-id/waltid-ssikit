package id.walt.services.sdjwt

import id.walt.credentials.selectiveDisclosure.SDField
import id.walt.servicematrix.BaseService
import id.walt.servicematrix.ServiceProvider
import kotlinx.serialization.json.JsonObject

abstract class SDJwtService: BaseService() {
    override val implementation get() = serviceImplementation<SDJwtService>()

    /**
     * Return combined SD-JWT for issuance with all selective disclosures appended
     * @param keyAlias  Alias of signing key
     * @param payload   Payload, with all fields disclosed
     * @param sdMap     Map indicating selective disclosability and disclosed/undisclosed state for each field
     */
    open fun sign(keyAlias: String, payload: JsonObject, sdMap: Map<String, SDField>): SDJwt
        = implementation.sign(keyAlias, payload, sdMap)

    /**
     * Verifies SD_JWT signature and validity of disclosures
     * @param sdJwt Combined SD_JWT with disclosures
     */
    open fun verify(sdJwt: SDJwt): Boolean
        = implementation.verify(sdJwt)

    /**
     * Returns JsonObject of payload with all disclosed fields recursively resolved
     * @param sdJwt Combined SD_JWT with disclosures
     */
    open fun disclosePayload(sdJwt: SDJwt): JsonObject
        = implementation.disclosePayload(sdJwt)

    /**
     * Returns map indicating for each field, whether it is selectively disclosable
     * @param combinedSdJwt Combined SD_JWT with disclosures
     */
    open fun toSDMap(sdJwt: SDJwt): Map<String, SDField>
        = implementation.toSDMap(sdJwt)

    /**
     * Returns combined sd-jwt with undisclosed (according to sdMap) fields removed from disclosures
     * @param sdJwt Original (as issued) SD-JWT combined with disclosures
     * @param sdMap map indicating, which selectively disclosable fields should be disclosed or undisclosed
     */
    open fun present(sdJwt: SDJwt, sdMap: Map<String, SDField>): SDJwt
        = implementation.present(sdJwt, sdMap)

    companion object : ServiceProvider {
        override fun getService() = object : SDJwtService() {}
        override fun defaultImplementation() = WaltIdSDJwtService()
    }
}
