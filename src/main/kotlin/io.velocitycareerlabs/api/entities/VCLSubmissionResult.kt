/**
 * Created by Michael Avoyan on 4/13/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

data class VCLSubmissionResult(val token: VCLToken, val exchange: VCLExchange) {

    companion object CodingKeys {
        const val KeyToken = "token"
        const val KeyExchange = "exchange"
    }
}