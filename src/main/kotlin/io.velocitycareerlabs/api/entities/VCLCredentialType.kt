/**
 * Created by Michael Avoyan on 3/21/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import org.json.JSONObject

data class VCLCredentialType (
    val payload: JSONObject,
        val id: String?,
        val schema: String?,
        val createdAt: String?,
        val schemaName: String?,
        val credentialType: String?,
        val recommended: Boolean
    ) {

    companion object CodingKeys {
        const val KeyId = "id"
        const val KeySchema = "schema"
        const val KeyCreatedAt = "createdAt"
        const val KeySchemaName = "schemaName"
        const val KeyCredentialType = "credentialType"
        const val KeyRecommended = "recommended"
    }
}