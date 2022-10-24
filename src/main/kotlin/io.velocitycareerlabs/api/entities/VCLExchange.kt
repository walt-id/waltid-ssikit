/**
 * Created by Michael Avoyan on 5/4/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

data class VCLExchange(val id: String, val type: String, val disclosureComplete: Boolean, val exchangeComplete: Boolean) {

    companion object CodingKeys {
        const val KeyId = "id"
        const val KeyType = "type"
        const val KeyDisclosureComplete = "disclosureComplete"
        const val KeyExchangeComplete = "exchangeComplete"
    }
}