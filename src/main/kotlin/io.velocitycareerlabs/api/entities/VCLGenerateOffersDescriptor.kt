/**
 * Created by Michael Avoyan on 10/05/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import io.velocitycareerlabs.impl.extensions.toJsonArray
import org.json.JSONObject

data class VCLGenerateOffersDescriptor(
    val credentialManifest: VCLCredentialManifest,
    val types: List<String>? = null,
    val offerHashes: List<String>? = null,
    val identificationVerifiableCredentials: List<VCLVerifiableCredential>
    ) {
    val payload: JSONObject =
        JSONObject()
            .putOpt(KeyExchangeId, exchangeId)
            .putOpt(KeyTypes,types?.toJsonArray())
            .putOpt(KeyOfferHashes,offerHashes?.toJsonArray())

    val did: String get() = credentialManifest.did
    val exchangeId: String get() = credentialManifest.exchangeId

    val checkOffersUri: String get() = credentialManifest.checkOffersUri

    companion object CodingKeys {
        const val KeyExchangeId = "exchangeId"
        const val KeyTypes = "types"
        const val KeyOfferHashes = "offerHashes"
    }
}
