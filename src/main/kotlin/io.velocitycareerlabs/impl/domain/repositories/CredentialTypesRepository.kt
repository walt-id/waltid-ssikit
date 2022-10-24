/**
 * Created by Michael Avoyan on 3/13/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.repositories

import io.velocitycareerlabs.api.entities.VCLCredentialTypes
import io.velocitycareerlabs.api.entities.VCLResult

internal interface CredentialTypesRepository {
    fun getCredentialTypes(completionBlock: (VCLResult<VCLCredentialTypes>) -> Unit)
}