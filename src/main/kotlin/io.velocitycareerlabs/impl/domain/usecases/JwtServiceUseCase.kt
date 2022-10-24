/**
 * Created by Michael Avoyan on 14/06/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.usecases

import io.velocitycareerlabs.api.entities.VCLJWT
import io.velocitycareerlabs.api.entities.VCLPublicKey
import io.velocitycareerlabs.api.entities.VCLResult
import org.json.JSONObject

internal interface JwtServiceUseCase {
    fun verifyJwt(jwt: VCLJWT, publicKey: VCLPublicKey, completionBlock: (VCLResult<Boolean>) -> Unit)
    fun generateSignedJwt(payload: JSONObject, iss: String, completionBlock: (VCLResult<VCLJWT>) -> Unit)
}