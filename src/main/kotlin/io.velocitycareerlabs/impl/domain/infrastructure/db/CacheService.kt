/**
 * Created by Michael Avoyan on 16/06/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.infrastructure.db

internal interface CacheService {
    var countryCodes: String?
    var stateCodes: String?
    var credentialTypes: String?
    fun getCredentialTypeSchema(schemaName: String): String?
    fun setCredentialTypeSchema(schemaName: String, schema: String)
}