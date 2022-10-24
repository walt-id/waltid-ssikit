/**
 * Created by Michael Avoyan on 09/05/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

data class VCLCredentialManifest(
    val jwt: VCLJWT,
    val vendorOriginContext: String? = null
) {
    val iss: String get() = jwt.payload.toJSONObject()?.get(KeyIss) as? String ?: ""
    val did: String get() = iss
    val exchangeId: String get() = jwt.payload.toJSONObject()?.get(KeyExchangeId) as? String ?: ""
    val presentationDefinitionId: String get() = (jwt.payload.toJSONObject()?.get(KeyPresentationDefinitionId) as? Map<*, *>)?.get(
        KeyId) as? String ?: ""

    val finalizeOffersUri: String get() =
        (jwt.payload.toJSONObject()?.get(VCLCredentialManifest.KeyMetadata) as? Map<*, *>)?.get(
            VCLCredentialManifest.KeyFinalizeOffersUri
        )?.toString() ?: ""

    val checkOffersUri: String get() =
        (jwt.payload.toJSONObject()?.get(VCLCredentialManifest.KeyMetadata) as? Map<*, *>)?.get(
            VCLCredentialManifest.KeyCheckOffersUri
        )?.toString() ?: ""

    val submitPresentationUri: String get() =
        (jwt.payload.toJSONObject()?.get(VCLCredentialManifest.KeyMetadata) as? Map<*, *>)?.get(
            VCLCredentialManifest.KeySubmitIdentificationUri
        )?.toString() ?: ""

    companion object CodingKeys {
        const val KeyIssuingRequest = "issuing_request"

        const val KeyId = "id"
        const val KeyIss = "iss"
        const val KeyIssuer = "issuer"
        const val KeyExchangeId = "exchange_id"
        const val KeyPresentationDefinitionId = "presentation_definition"

        const val KeyMetadata = "metadata"
        const val KeyCheckOffersUri = "check_offers_uri"
        const val KeyFinalizeOffersUri = "finalize_offers_uri"
        const val KeySubmitIdentificationUri = "submit_presentation_uri"
    }
}
