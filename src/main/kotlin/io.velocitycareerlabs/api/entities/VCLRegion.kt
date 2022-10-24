/**
 * Created by Michael Avoyan on 06/05/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import org.json.JSONObject

data class VCLRegion(
    override val payload: JSONObject,
    override val code: String,
    override val name: String
    ): VCLPlace {

    companion object Codes {
        const val KeyCode = "code"
        const val KeyName = "name"
    }
}