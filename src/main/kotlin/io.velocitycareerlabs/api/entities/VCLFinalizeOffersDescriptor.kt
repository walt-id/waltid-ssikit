/**
 * Created by Michael Avoyan on 11/05/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import io.velocitycareerlabs.impl.extensions.toJsonArray
import org.json.JSONObject

data class VCLFinalizeOffersDescriptor(
    val credentialManifest: VCLCredentialManifest,
    val approvedOfferIds: List<String>,
    val rejectedOfferIds: List<String>
) {
    val payload: JSONObject =
        JSONObject()
            .putOpt(KeyExchangeId, exchangeId)
            .putOpt(KeyApprovedOfferIds, approvedOfferIds.toJsonArray())
            .putOpt(KeyRejectedOfferIds, rejectedOfferIds.toJsonArray())

    val did: String get() = credentialManifest.did
    val exchangeId: String get() = credentialManifest.exchangeId

    val finalizeOffersUri: String get() = credentialManifest.finalizeOffersUri

    companion object CodingKeys {
        const val KeyExchangeId = "exchangeId"
        const val KeyApprovedOfferIds = "approvedOfferIds"
        const val KeyRejectedOfferIds = "rejectedOfferIds"
    }
}
