/**
 * Created by Michael Avoyan on 13/06/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import org.json.JSONObject

data class VCLCredentialTypesUIFormSchema(val payload: JSONObject) {
    companion object CodingKeys {
        const val KeyAddressRegion = "addressRegion"
        const val KeyAddressCountry = "addressCountry"
        const val KeyUiEnum = "ui:enum"
        const val KeyUiNames = "ui:enumNames"
    }
}
