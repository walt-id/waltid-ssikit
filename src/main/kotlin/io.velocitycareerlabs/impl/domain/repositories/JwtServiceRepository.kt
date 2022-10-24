/**
 * Created by Michael Avoyan on 4/6/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.repositories

import io.velocitycareerlabs.api.entities.VCLJWT
import io.velocitycareerlabs.api.entities.VCLPublicKey
import io.velocitycareerlabs.api.entities.VCLResult
import org.json.JSONObject

internal interface JwtServiceRepository {
    fun decode(encodedJwt: String, completionBlock: (VCLResult<VCLJWT>) -> Unit)
    fun verifyJwt(jwt: VCLJWT, publicKey: VCLPublicKey, completionBlock: (VCLResult<Boolean>) -> Unit)
    fun generateSignedJwt(payload: JSONObject, iss: String, completionBlock: (VCLResult<VCLJWT>) -> Unit)
}