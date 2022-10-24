/**
 * Created by Michael Avoyan on 4/20/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import io.velocitycareerlabs.impl.extensions.toJsonObject
import org.json.JSONObject

public class VCLPublicKey {
    public val jwkStr: String
    public val jwkJson: JSONObject

    constructor(jwkStr: String) {
        this.jwkStr = jwkStr
        this.jwkJson = this.jwkStr.toJsonObject() ?: JSONObject("{}")
    }

    constructor(jwkJson: JSONObject) {
        this.jwkJson = jwkJson
        this.jwkStr = this.jwkJson.toString()
    }

    enum class Format(val value: String) {
        jwk("jwk"),
        hex("hex"),
        pem("pem"),
        base58("base58")
    }

    override operator fun equals(other: Any?) =
        this.jwkStr == (other as? VCLPublicKey)?.jwkStr &&
                this.jwkJson.toString() == (other as? VCLPublicKey)?.jwkJson.toString()
}