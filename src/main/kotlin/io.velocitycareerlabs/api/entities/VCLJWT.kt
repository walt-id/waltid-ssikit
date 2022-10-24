/**
 * Created by Michael Avoyan on 4/26/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.Payload
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.SignedJWT

class VCLJWT {
    val signedJwt: SignedJWT

    constructor(signedJwt: SignedJWT) {
        this.signedJwt = signedJwt
    }

    constructor(encodedJwt: String) {
        val encodedJwtArr = encodedJwt.split(".")
        this.signedJwt = SignedJWT(
            Base64URL(if(encodedJwtArr.isNotEmpty()) encodedJwtArr.component1() else ""),
            Base64URL(if(encodedJwtArr.size >= 2) encodedJwtArr.component2() else ""),
            Base64URL(if(encodedJwtArr.size >= 3) encodedJwtArr.component3() else "")
        )
    }

    val header: JWSHeader get() = signedJwt.header
    val payload: Payload get() = signedJwt.payload
    val signature: Base64URL get() = signedJwt.signature
}