/**
 * Created by Michael Avoyan on 4/23/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.extensions

import com.nimbusds.jwt.JWTClaimsSet
import org.json.JSONObject

internal fun JWTClaimsSet.Builder.addClaims(jsonObj: JSONObject) {
    val mapObj = jsonObj.toMap()
    mapObj.map {
        this.claim(it.key, it.value)
    }
}