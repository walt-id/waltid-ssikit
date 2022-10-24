/**
 * Created by Michael Avoyan on 28/10/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import org.json.JSONObject

data class VCLVerifiedProfile(val payload: JSONObject) {

    val credentialSubject: JSONObject? get() = payload.optJSONObject(VCLVerifiedProfile.KeyCredentialSubject)

    val name get() = credentialSubject?.optString(VCLVerifiedProfile.KeyName)
    val logo get() = credentialSubject?.optString(VCLVerifiedProfile.KeyLogo)
    val id get() = credentialSubject?.optString(VCLVerifiedProfile.KeyId)

    companion object CodingKeys {
        const val KeyCredentialSubject = "credentialSubject"

        const val KeyName = "name"
        const val KeyLogo = "logo"
        const val KeyId = "id"
    }
}