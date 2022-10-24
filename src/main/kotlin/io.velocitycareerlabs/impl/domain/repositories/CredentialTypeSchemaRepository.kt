/**
 * Created by Michael Avoyan on 3/30/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.repositories

import io.velocitycareerlabs.api.entities.VCLCredentialTypeSchema
import io.velocitycareerlabs.api.entities.VCLResult

internal interface CredentialTypeSchemaRepository {
    fun getCredentialTypeSchema(schemaName: String, completionBlock: (VCLResult<VCLCredentialTypeSchema>) -> Unit)
}