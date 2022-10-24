/**
 * Created by Michael Avoyan on 3/11/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import org.json.JSONObject

open class VCLService(val payload: JSONObject) {
    val id: String = payload.getString(VCLService.KeyId)
    val type: String = payload.getString(VCLService.KeyType)
    val serviceEndpoint: String = payload.getString(VCLService.KeyServiceEndpoint)

    companion object CodingKeys {
        const val KeyId = "id"
        const val KeyType = "type"
        const val KeyCredentialTypes = "credentialTypes"
        const val KeyServiceEndpoint = "serviceEndpoint"
    }
}