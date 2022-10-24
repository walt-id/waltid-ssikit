/**
 * Created by Michael Avoyan on 24/06/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import org.json.JSONObject

interface VCLPlace {
    val payload: JSONObject
    val code: String
    val name: String
}